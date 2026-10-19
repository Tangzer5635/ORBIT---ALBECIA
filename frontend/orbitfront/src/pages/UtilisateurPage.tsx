import {useEffect, useState} from "react";
import {
    LIBELLE_ROLE,
    type Page,
    type Role,
    type Utilisateur,
    type UtilisateurRequest,
} from "../types/types.ts";
import {
    ajouterUtilisateur,
    changerVerrou,
    compterUtilisateurs,
    type FiltresUtilisateur,
    getUtilisateurs,
    modifierUtilisateur,
    supprimerUtilisateur,
    UtilisateurError,
} from "../services/utilisateurService.ts";
import {getUtilisateur} from "../services/authService.ts";
import UtilisateurLigne from "../components/Utilisateur/UtilisateurLigne.tsx";
import Popup from "../components/Popup/Popup.tsx";
import {rechercheVersRsql} from "../utils/utilisateur.ts";

const ROLES = Object.keys(LIBELLE_ROLE) as Role[];
// Boutons "état du compte" : chacun correspond à des filtres booléens envoyés au back
type EtatCompte = "actifs" | "inactifs" | "verrouilles";
const ETATS: { cle: EtatCompte; libelle: string; filtres: FiltresUtilisateur }[] = [
    { cle: "actifs", libelle: "Actifs", filtres: { active: true } },
    { cle: "inactifs", libelle: "Inactifs", filtres: { active: false } },
    { cle: "verrouilles", libelle: "Verrouillés", filtres: { verrouille: true } },
];
const TAILLE_PAGE = 20;

interface Compteurs {
    parRole: number[];   // dans l'ordre de ROLES
    inactifs: number;
    verrouilles: number;
}

// Les compteurs ne dépendent pas des filtres : une requête "page de 1" par compteur, on lit totalElements
// (même principe que chargerCompteurs dans MaterielPage)
async function chargerCompteurs(): Promise<Compteurs> {
    const [inactifs, verrouilles, ...parRole] = await Promise.all([
        compterUtilisateurs({active: false}),
        compterUtilisateurs({verrouille: true}),
        ...ROLES.map((r) => compterUtilisateurs({role: r})),
    ]);
    return {parRole, inactifs, verrouilles};
}

// Popup ouverte (même principe que BatimentsPage, avec en plus la modification)
type PopupState =
    | { type: "creer" }
    | { type: "modifier"; utilisateur: Utilisateur }
    | { type: "supprimer"; utilisateur: Utilisateur }
    | null;

export default function UtilisateurPage() {
    // données renvoyées par le back
    const [resultat, setResultat] = useState<Page<Utilisateur>>(); // la page affichée
    const [compteurs, setCompteurs] = useState<Compteurs>();
    const [erreurChargement, setErreurChargement] = useState<string>();
    const [error, setError] = useState<string>(); // erreur hors popup (verrouillage)
    const [version, setVersion] = useState(0);     // +1 = recharger la liste et les compteurs
    const moi = getUtilisateur(); // utilisateur connecté

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
    const [saisie, setSaisie] = useState("");         // ce qui est tapé dans le champ
    const [recherche, setRecherche] = useState("");   // ce qui est envoyé au back
    const [filtreRole, setFiltreRole] = useState<Role | null>(null);
    const [filtreEtat, setFiltreEtat] = useState<EtatCompte | null>(null);
    const [page, setPage] = useState(0);              // numéro de page, commence à 0

    // la recherche part 500 ms après la dernière frappe pour ne pas faire une requête par lettre
    useEffect(() => {
        const minuteur = setTimeout(() => {
            setRecherche(saisie);
            setPage(0);
        }, 500);
        return () => clearTimeout(minuteur);
    }, [saisie]);

    // recharge la page à chaque changement de filtre, de page, ou après une action (version)
    useEffect(() => {
        let actif = true; // pour ignorer une réponse qui arrive après un changement de filtre
        const etat = ETATS.find((e) => e.cle === filtreEtat)?.filtres ?? {}; // ex. { active: false }
        getUtilisateurs({filter: rechercheVersRsql(recherche), role: filtreRole, ...etat}, page, TAILLE_PAGE)
            .then((p) => {
                if (!actif) return;
                // la dernière page vient d'être vidée (suppression) : on revient à la précédente
                if (p.content.length === 0 && page > 0) setPage(page - 1);
                else setResultat(p);
                setErreurChargement(undefined);
            })
            .catch(() => {
                if (actif) setErreurChargement("Impossible de charger les utilisateurs.");
            });
        return () => { actif = false; };
    }, [recherche, filtreRole, filtreEtat, page, version]);

    // Les compteurs ne changent pas avec les filtres, seulement après une action
    useEffect(() => {
        chargerCompteurs().then(setCompteurs).catch(() => {});
    }, [version]);

    // après une création, modification, suppression ou un verrouillage : on redemande au back
    function recharger() {
        setVersion((v) => v + 1);
    }

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
            if (aModifier) await modifierUtilisateur(aModifier.id, dto, aModifier);            else await ajouterUtilisateur(dto);
            fermerPopup();
            recharger();
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
            fermerPopup();
            recharger();
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
            await changerVerrou(u.id, !u.verrouille);
            recharger();
        } catch (err) {
            if (err instanceof UtilisateurError && err.status === 400) setError(err.detail || "Action impossible sur ce compte.");
            else if (err instanceof UtilisateurError && err.status === 403) setError("Vous n'avez pas le droit de verrouiller un compte.");
            else setError("Le service est indisponible. Réessayez plus tard.");
        }
    }

    // ---------- affichage ----------

    // Utilisateur concerné par la popup (undefined si on est en création)
    const aModifier = popup?.type === "modifier" ? popup.utilisateur : undefined;
    const aSupprimer = popup?.type === "supprimer" ? popup.utilisateur : undefined;
    const estMoi = aModifier !== undefined && aModifier.id === moi?.id; // pas de changement de son propre rôle / état

    // un changement de filtre ramène à la première page
    function changerRole(r: Role | null) {
        setFiltreRole(r);
        setPage(0);
    }

    function changerEtat(e: EtatCompte | null) {
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
                {resultat && (
                    <span className="muted">{resultat.totalElements} utilisateur{resultat.totalElements > 1 ? "s" : ""}</span>
                )}
            </div>

            {/* compteurs par rôle */}
            <div className="stats">
                {ROLES.map((r, i) => (
                    <div key={r} className="stat">
                        <b>{compteurs ? compteurs.parRole[i] : "–"}</b>
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
                value={saisie}
                onChange={(e) => setSaisie(e.target.value)}
            />

            <div className="filters">
                <div className="seg" role="group" aria-label="Rôle">
                    <button type="button" aria-pressed={filtreRole === null} onClick={() => changerRole(null)}>
                        Tous les rôles
                    </button>
                    {ROLES.map((r) => (
                        <button key={r} type="button" aria-pressed={filtreRole === r} onClick={() => changerRole(r)}>
                            {LIBELLE_ROLE[r]}
                        </button>
                    ))}
                </div>
                <div className="seg" role="group" aria-label="État du compte">
                    <button type="button" aria-pressed={filtreEtat === null} onClick={() => changerEtat(null)}>
                        Actifs et inactifs
                    </button>
                    {ETATS.map((e) => (
                        <button key={e.cle} type="button" aria-pressed={filtreEtat === e.cle} onClick={() => changerEtat(e.cle)}>
                            {e.libelle}
                            {e.cle === "inactifs" && compteurs && ` (${compteurs.inactifs})`}
                            {e.cle === "verrouilles" && compteurs && ` (${compteurs.verrouilles})`}
                        </button>
                    ))}
                </div>
            </div>

            <div className="list">
                {resultat?.content.map((u) => (
                    <UtilisateurLigne
                        key={u.id}
                        utilisateur={u}
                        estMoi={u.id === moi?.id}
                        onModifier={ouvrirModification}
                        onVerrou={handleVerrou}
                        onSupprime={ouvrirSuppression}
                    />
                ))}
                {!resultat && !erreurChargement && <p className="muted">Chargement…</p>}
                {resultat && resultat.totalElements === 0 && <p className="muted">Aucun utilisateur ne correspond.</p>}
            </div>

            {/* boutons de changement de page (même principe que MaterielPage) */}
            {resultat && resultat.totalPages > 1 && (
                <nav className="pagination" aria-label="Pagination">
                    <button type="button" className="btn sm" disabled={resultat.first} onClick={() => setPage((p) => p - 1)}>
                        ‹ Précédent
                    </button>
                    <span className="muted small">Page {resultat.page + 1} sur {resultat.totalPages}</span>
                    <button type="button" className="btn sm" disabled={resultat.last} onClick={() => setPage((p) => p + 1)}>
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