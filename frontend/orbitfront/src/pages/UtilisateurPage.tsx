import {useEffect, useMemo, useState} from "react";
import {LIBELLE_ROLE, type Role, type Utilisateur, type UtilisateurRequest} from "../types/types.ts";
import {
    ajouterUtilisateur,
    changerVerrou,
    getUtilisateurs,
    modifierUtilisateur,
    supprimerUtilisateur,
    UtilisateurError,
} from "../services/utilisateurService.ts";
import {getUtilisateur} from "../services/authService.ts";
import UtilisateurLigne from "../components/UtilisateurLigne.tsx";
import Popup from "../components/Popup.tsx";

const ROLES = Object.keys(LIBELLE_ROLE) as Role[];
const PAR_PAGE = 30;

// Popup ouverte (même principe que BatimentsPage, avec en plus la modification)
type PopupState =
    | { type: "creer" }
    | { type: "modifier"; utilisateur: Utilisateur }
    | { type: "supprimer"; utilisateur: Utilisateur }
    | null;

type FiltreEtat = "" | "actifs" | "inactifs" | "verrouilles";

export default function UtilisateurPage() {
    const [nonTries, setUtilisateurs] = useState<Utilisateur[]>([]);
    const [erreurChargement, setErreurChargement] = useState<string>();
    const [error, setError] = useState<string>(); // erreur hors popup (verrouillage)
    const moi = getUtilisateur(); // utilisateur connecté
    const [page, setPage] = useState(0); // numéro de page, commence à 0

    // popup + champs du formulaire
    const [popup, setPopup] = useState<PopupState>(null);
    const [prenom, setPrenom] = useState("");
    const [nom, setNom] = useState("");
    const [login, setLogin] = useState("");
    const [nid, setNid] = useState("");
    const [role, setRole] = useState<Role>("STAGIAIRE");
    const [motDePasse, setMotDePasse] = useState("");
    const [actif, setActif] = useState(true);
    const [erreur, setErreur] = useState("");
    const [envoi, setEnvoi] = useState(false);

    // filtres
    const [recherche, setRecherche] = useState("");
    const [filtreRole, setFiltreRole] = useState<Role | "">("");
    const [filtreEtat, setFiltreEtat] = useState<FiltreEtat>("");

    // Chargement de la liste à l'ouverture de la page
    useEffect(() => {
        getUtilisateurs()
            .then(setUtilisateurs)
            .catch(() => setErreurChargement("Impossible de charger les utilisateurs."));
    }, []);

    // Tri par nom puis prénom
    const utilisateurs = useMemo(
        () => [...nonTries].sort((a, b) =>
            a.nom.localeCompare(b.nom, "fr") || a.prenom.localeCompare(b.prenom, "fr")),
        [nonTries]
    );

    // ---------- ouverture / fermeture des popups ----------

    // remplit les champs (vides en création, ceux de l'utilisateur en modification)
    function remplirChamps(u?: Utilisateur) {
        setPrenom(u?.prenom ?? "");
        setNom(u?.nom ?? "");
        setLogin(u?.login ?? "");
        setNid("");
        setRole(u?.role ?? "STAGIAIRE");
        setMotDePasse("");
        setActif(u?.active ?? true);
        setErreur("");
    }

    function ouvrirCreation() {
        remplirChamps();
        setPopup({type: "creer"});
    }

    function ouvrirModification(utilisateur: Utilisateur) {
        remplirChamps(utilisateur);
        setPopup({type: "modifier", utilisateur});
    }

    function ouvrirSuppression(utilisateur: Utilisateur) {
        setErreur("");
        setPopup({type: "supprimer", utilisateur});
    }

    function fermerPopup() {
        setPopup(null);
        remplirChamps();
    }

    function remplacer(maj: Utilisateur) {
        setUtilisateurs((liste) => liste.map((x) => (x.id === maj.id ? maj : x)));
    }

    // ---------- actions ----------

    // Mêmes règles que l'entité Personnel côté back
    function verifier(creation: boolean): string {
        if (prenom.trim() === "" || prenom.trim().length > 30) return "Le prénom est obligatoire (30 caractères maximum).";
        if (nom.trim() === "" || nom.trim().length > 50) return "Le nom est obligatoire (50 caractères maximum).";
        if (login.trim().length < 3 || login.trim().length > 50) return "L'identifiant doit faire entre 3 et 50 caractères.";
        if (creation && !/^[0-9]{10}$/.test(nid.trim())) return "Le NID doit contenir exactement 10 chiffres.";
        if (creation && motDePasse === "") return "Le mot de passe provisoire est obligatoire.";
        return "";
    }

    // Création et modification passent par la même fonction
    async function handleEnregistrer(aModifier?: Utilisateur) {
        const message = verifier(!aModifier);
        if (message) {
            setErreur(message);
            return;
        }

        const dto: UtilisateurRequest = {
            ...(aModifier ? {} : {nid: nid.trim()}), // en modification, le back garde le NID existant
            nom: nom.trim(),
            prenom: prenom.trim(),
            login: login.trim(),
            role,
            active: actif,
            ...(motDePasse ? {motDePasse} : {}), // vide = mot de passe inchangé
        };

        setEnvoi(true);
        try {
            if (aModifier) {
                remplacer(await modifierUtilisateur(aModifier.id, dto));
            } else {
                const nouveau = await ajouterUtilisateur(dto);
                setUtilisateurs((liste) => [...liste, nouveau]);
            }
            fermerPopup();
        } catch (err) {
            if (err instanceof UtilisateurError && err.status === 403) setErreur("Vous n'avez pas le droit de gérer les utilisateurs.");
            else if (err instanceof UtilisateurError && err.status === 400) setErreur(err.detail || "Informations invalides.");
            else if (err instanceof UtilisateurError && err.status === 404) setErreur("Cet utilisateur n'a pas été créer.");
            else setErreur("Le service est indisponible. Réessayez plus tard.");
        } finally {
            setEnvoi(false);
        }
    }

    async function handleSupprimer(u: Utilisateur) {
        setEnvoi(true);
        try {
            await supprimerUtilisateur(u.id);
            setUtilisateurs((liste) => liste.filter((x) => x.id !== u.id));
            fermerPopup();
        } catch (err) {
            if (err instanceof UtilisateurError && err.status === 400) setErreur(err.detail || "Suppression impossible.");
            else if (err instanceof UtilisateurError && err.status === 403) setErreur("Vous n'avez pas le droit de supprimer un utilisateur.");
            else if (err instanceof UtilisateurError && err.status === 404) setErreur("Cet utilisateur n'existe plus.");
            // 500 : en pratique le compte est encore lié à un poste ou à des défaillances (clé étrangère)
            else if (err instanceof UtilisateurError && err.status === 500) setErreur("Ce compte occupe un poste ou a des défaillances à son nom. Désactivez-le plutôt.");
            else setErreur("Le service est indisponible. Réessayez plus tard.");
        } finally {
            setEnvoi(false);
        }
    }

    async function handleVerrou(u: Utilisateur) {
        setError(undefined);
        try {
            remplacer(await changerVerrou(u.id, !u.verrouille));
        } catch (err) {
            if (err instanceof UtilisateurError && err.status === 400) setError(err.detail || "Action impossible sur ce compte.");
            else if (err instanceof UtilisateurError && err.status === 403) setError("Vous n'avez pas le droit de verrouiller un compte.");
            else setError("Le service est indisponible. Réessayez plus tard.");
        }
    }

    // ---------- affichage ----------

    // liste filtrée
    const q = recherche.trim().toLowerCase();
    const liste = utilisateurs.filter((u) =>
        (filtreRole === "" || u.role === filtreRole) &&
        (filtreEtat !== "actifs" || u.active) &&
        (filtreEtat !== "inactifs" || !u.active) &&
        (filtreEtat !== "verrouilles" || u.verrouille) &&
        (q === "" || `${u.prenom} ${u.nom} ${u.login} ${u.id}`.toLowerCase().includes(q))
    );
    // pagination
    const nbPages = Math.max(1, Math.ceil(liste.length / PAR_PAGE));
    const pageCourante = Math.min(page, nbPages - 1); // si un filtre réduit la liste, on ne sort pas des pages
    const visibles = liste.slice(pageCourante * PAR_PAGE, (pageCourante + 1) * PAR_PAGE);
    const nbInactifs = utilisateurs.filter((u) => !u.active).length;
    const nbVerrouilles = utilisateurs.filter((u) => u.verrouille).length;

    // Utilisateur concerné par la popup (undefined si on est en création)
    const aModifier = popup?.type === "modifier" ? popup.utilisateur : undefined;
    const aSupprimer = popup?.type === "supprimer" ? popup.utilisateur : undefined;
    const estMoi = aModifier !== undefined && aModifier.id === moi?.id; // pas de changement de son propre rôle / état

    // un changement de filtre ramène à la première page
    function choisirRole(r: Role | "") {
        setFiltreRole(r);
        setPage(0);
    }

    function choisirEtat(e: FiltreEtat) {
        setFiltreEtat(e);
        setPage(0);
    }

    // efface l'erreur dès qu'on retape quelque chose
    function champ(set: (v: string) => void) {
        return (e: { target: { value: string } }) => {
            set(e.target.value);
            setErreur("");
        };
    }

    return (
        <>
            <div className="page-head">
                <h1>Utilisateurs</h1>
                <span className="muted">{liste.length} utilisateur{liste.length > 1 ? "s" : ""}</span>
            </div>

            {/* compteurs par rôle */}
            <div className="stats">
                {ROLES.map((r) => (
                    <div key={r} className="stat">
                        <b>{utilisateurs.filter((u) => u.role === r).length}</b>
                        <span>{LIBELLE_ROLE[r]}s</span>
                    </div>
                ))}
            </div>

            <button type="button" className="btn primary" onClick={ouvrirCreation}>
                Créer un utilisateur
            </button>

            {(error ?? erreurChargement) && <p className="error-message" role="alert">{error ?? erreurChargement}</p>}

            <input
                className="search"
                type="search"
                placeholder="Rechercher un utilisateur par nom ou identifiant"
                aria-label="Rechercher un utilisateur"
                value={recherche}
                onChange={(e) => {
                    setRecherche(e.target.value);
                    setPage(0);
                }}
            />

            <div className="filters">
                <div className="seg" role="group" aria-label="Rôle">
                    <button type="button" aria-pressed={filtreRole === ""} onClick={() => choisirRole("")}>Tous les
                        rôles
                    </button>
                    {ROLES.map((r) => (
                        <button key={r} type="button" aria-pressed={filtreRole === r}
                                onClick={() => choisirRole(r)}>{LIBELLE_ROLE[r]}</button>
                    ))}
                </div>
                <div className="seg" role="group" aria-label="État du compte">
                    <button type="button" aria-pressed={filtreEtat === ""} onClick={() => choisirEtat("")}>Actifs et
                        inactifs
                    </button>
                    <button type="button" aria-pressed={filtreEtat === "actifs"}
                            onClick={() => choisirEtat("actifs")}>Actifs
                    </button>
                    <button type="button" aria-pressed={filtreEtat === "inactifs"}
                            onClick={() => choisirEtat("inactifs")}>Inactifs ({nbInactifs})
                    </button>
                    <button type="button" aria-pressed={filtreEtat === "verrouilles"}
                            onClick={() => choisirEtat("verrouilles")}>Verrouillés ({nbVerrouilles})
                    </button>
                </div>
            </div>

            <div className="list">
                {visibles.map((u) => (
                    <UtilisateurLigne
                        key={u.id}
                        utilisateur={u}
                        estMoi={u.id === moi?.id}
                        onModifier={ouvrirModification}
                        onVerrou={handleVerrou}
                        onSupprime={ouvrirSuppression}
                    />
                ))}
                {liste.length === 0 && !erreurChargement && <p className="muted">Aucun utilisateur ne correspond.</p>}
            </div>

            {nbPages > 1 && (
                <nav className="pagination" aria-label="Pages">
                    <button type="button" className="btn sm" onClick={() => setPage(pageCourante - 1)}
                            disabled={pageCourante === 0}>
                        ‹ Précédent
                    </button>
                    <span className="muted small">
                        Page {pageCourante + 1} sur {nbPages} ({liste.length} utilisateurs)
                    </span>
                    <button type="button" className="btn sm" onClick={() => setPage(pageCourante + 1)}
                            disabled={pageCourante === nbPages - 1}>
                        Suivant ›
                    </button>
                </nav>
            )}

            <Popup
                open={popup !== null}
                onClose={fermerPopup}
                title={aSupprimer
                    ? `Supprimer ${aSupprimer.prenom} ${aSupprimer.nom} ?`
                    : aModifier ? "Modifier l’utilisateur" : "Créer un utilisateur"}
                description={aSupprimer
                    ? `Le compte ${aSupprimer.login} sera supprimé définitivement. Pour bloquer l’accès sans supprimer, désactivez-le ou verrouillez-le.`
                    : aModifier
                        ? <>Identifiant actuel : <b>{aModifier.login}</b> (ID {aModifier.id})</>
                        : "Donnez un mot de passe provisoire, à changer à la première connexion."}
                confirmLabel={aSupprimer ? "Supprimer l’utilisateur" : aModifier ? "Enregistrer" : "Créer l’utilisateur"}
                variant={aSupprimer ? "danger" : "primary"}
                onConfirm={aSupprimer ? () => handleSupprimer(aSupprimer) : () => handleEnregistrer(aModifier)}
                loading={envoi}
            >
                {!aSupprimer && (
                    <>
                        <div className="user-grille">
                            <div>
                                <label htmlFor="uPrenom">Prénom</label>
                                <input id="uPrenom" value={prenom} onChange={champ(setPrenom)} autoFocus/>
                            </div>
                            <div>
                                <label htmlFor="uNom">Nom</label>
                                <input id="uNom" value={nom} onChange={champ(setNom)}/>
                            </div>
                            <div>
                                <label htmlFor="uLogin">Identifiant</label>
                                <input id="uLogin" value={login} onChange={champ(setLogin)} autoComplete="off"/>
                            </div>
                            {!aModifier && (
                                <div>
                                    <label htmlFor="uNid">NID</label>
                                    <input id="uNid" value={nid} onChange={champ(setNid)} inputMode="numeric"
                                           maxLength={10} placeholder="10 chiffres"/>
                                </div>
                            )}
                            <div>
                                <label htmlFor="uRole">Rôle</label>
                                <select id="uRole" value={role} onChange={(e) => setRole(e.target.value as Role)}
                                        disabled={estMoi}>
                                    {ROLES.map((r) => <option key={r} value={r}>{LIBELLE_ROLE[r]}</option>)}
                                </select>
                            </div>
                            <div>
                                <label
                                    htmlFor="uMdp">{aModifier ? "Nouveau mot de passe" : "Mot de passe provisoire"}</label>
                                <input
                                    id="uMdp"
                                    type="password"
                                    value={motDePasse}
                                    onChange={champ(setMotDePasse)}
                                    autoComplete="new-password"
                                    placeholder={aModifier ? "Laisser vide pour ne pas changer" : ""}
                                />
                            </div>
                        </div>

                        {aModifier && (
                            <label className="user-check">
                                <input type="checkbox" checked={actif} onChange={(e) => setActif(e.target.checked)}
                                       disabled={estMoi}/>
                                <span>Compte actif. Décoché, l’utilisateur ne peut plus se connecter mais son historique est conservé.</span>
                            </label>
                        )}
                        {estMoi &&
                            <p className="muted small">Vous ne pouvez pas changer votre propre rôle ni désactiver votre
                                compte.</p>}
                    </>
                )}

                {erreur && <p className="error-message" role="alert">{erreur}</p>}
            </Popup>
        </>
    );
}