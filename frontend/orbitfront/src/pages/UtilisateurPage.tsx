import {useEffect, useState} from "react";
import {LIBELLE_ROLE, type Page, type Role, type Utilisateur} from "../types/types.ts";
import {
    changerVerrou,
    compterUtilisateurs,
    getUtilisateurs,
    supprimerUtilisateur,
    UtilisateurError,
} from "../services/utilisateurService.ts";
import {getUtilisateur} from "../services/authService.ts";
import {type FiltreEtat, versRsqlUtilisateurs} from "../utils/utilisateurs.ts";
import UtilisateurLigne from "../components/UtilisateurLigne.tsx";
import UtilisateurPopup from "../components/UtilisateurPopup.tsx";
import Popup from "../components/Popup.tsx";

const ROLES = Object.keys(LIBELLE_ROLE) as Role[];
const PAR_PAGE = 30;

// Popup ouverte (même principe que BatimentsPage, avec en plus la modification)
type PopupState =
    | { type: "creer" }
    | { type: "modifier"; utilisateur: Utilisateur }
    | { type: "supprimer"; utilisateur: Utilisateur }
    | null;

interface Compteurs {
    parRole: number[];    // dans l'ordre de ROLES
    inactifs: number;
    verrouilles: number;
}

// Les compteurs ne dépendent pas des filtres : quelques requêtes "size=1" lancées en parallèle
async function chargerCompteurs(): Promise<Compteurs> {
    const [inactifs, verrouilles, ...parRole] = await Promise.all([
        compterUtilisateurs("active==false"),
        compterUtilisateurs("verrouille==true"),
        ...ROLES.map((r) => compterUtilisateurs(`role==${r}`)),
    ]);
    return {parRole, inactifs, verrouilles};
}

export default function UtilisateurPage() {
    // données
    const [resultat, setResultat] = useState<Page<Utilisateur>>(); // page courante renvoyée par le back
    const [compteurs, setCompteurs] = useState<Compteurs>();
    const [version, setVersion] = useState(0); // +1 après chaque modification : recharge la page et les compteurs
    const [erreurChargement, setErreurChargement] = useState<string>();
    const [error, setError] = useState<string>(); // erreur hors popup (verrouillage)
    const moi = getUtilisateur(); // utilisateur connecté

    // popup ouverte + état de la popup de suppression (le formulaire est dans UtilisateurPopup)
    const [popup, setPopup] = useState<PopupState>(null);
    const [erreur, setErreur] = useState("");
    const [envoi, setEnvoi] = useState(false);

    // filtres et page
    const [saisie, setSaisie] = useState("");         // ce qui est tapé dans le champ
    const [recherche, setRecherche] = useState("");   // ce qui est envoyé au back
    const [filtreRole, setFiltreRole] = useState<Role | "">("");
    const [filtreEtat, setFiltreEtat] = useState<FiltreEtat>("");
    const [page, setPage] = useState(0); // numéro de page, commence à 0

    // la recherche part 300 ms après la dernière frappe pour ne pas faire une requête par lettre
    useEffect(() => {
        const minuteur = setTimeout(() => {
            setRecherche(saisie);
            setPage(0);
        }, 300);
        return () => clearTimeout(minuteur);
    }, [saisie]);

    // recharge la page à chaque changement de filtre, de page ou après une modification
    // (le tri nom, prénom, id est fait par le back)
    useEffect(() => {
        let actif = true; // pour ignorer une réponse qui arrive après un changement de filtre
        getUtilisateurs(versRsqlUtilisateurs(recherche, filtreRole, filtreEtat), page, PAR_PAGE)
            .then((p) => {
                if (actif) {
                    setResultat(p);
                    setErreurChargement(undefined);
                }
            })
            .catch(() => {
                if (actif) setErreurChargement("Impossible de charger les utilisateurs.");
            });
        return () => { actif = false; };
    }, [recherche, filtreRole, filtreEtat, page, version]);

    useEffect(() => {
        chargerCompteurs().then(setCompteurs).catch(() => {});
    }, [version]);

    // ---------- ouverture / fermeture des popups ----------

    function ouvrirCreation() {
        setPopup({type: "creer"});
    }

    function ouvrirModification(utilisateur: Utilisateur) {
        setPopup({type: "modifier", utilisateur});
    }

    function ouvrirSuppression(utilisateur: Utilisateur) {
        setErreur("");
        setPopup({type: "supprimer", utilisateur});
    }

    function fermerPopup() {
        setPopup(null);
        setErreur("");
    }

    // après une création, modification, suppression ou un verrouillage : on redemande la page au back
    // (l'utilisateur peut avoir changé de place dans le tri ou ne plus correspondre aux filtres)
    function rafraichir() {
        setVersion((v) => v + 1);
    }

    // appelé par UtilisateurPopup quand le back a bien enregistré
    function enregistre() {
        rafraichir();
        setPopup(null);
    }

    // ---------- actions ----------

    async function handleSupprimer(u: Utilisateur) {
        setEnvoi(true);
        try {
            await supprimerUtilisateur(u.id);
            rafraichir();
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
            await changerVerrou(u.id, !u.verrouille);
            rafraichir();
        } catch (err) {
            if (err instanceof UtilisateurError && err.status === 400) setError(err.detail || "Action impossible sur ce compte.");
            else if (err instanceof UtilisateurError && err.status === 403) setError("Vous n'avez pas le droit de verrouiller un compte.");
            else setError("Le service est indisponible. Réessayez plus tard.");
        }
    }

    // un changement de filtre ramène à la première page
    function choisirRole(r: Role | "") {
        setFiltreRole(r);
        setPage(0);
    }

    function choisirEtat(e: FiltreEtat) {
        setFiltreEtat(e);
        setPage(0);
    }

    // ---------- affichage ----------

    // filtrage et pagination sont faits par le back : on affiche la page reçue
    const visibles = resultat?.content ?? [];
    const nbResultats = resultat?.totalElements ?? 0;

    // Utilisateur concerné par la popup
    const aModifier = popup?.type === "modifier" ? popup.utilisateur : undefined;
    const aSupprimer = popup?.type === "supprimer" ? popup.utilisateur : undefined;
    const estMoi = aModifier !== undefined && aModifier.id === moi?.id; // pas de changement de son propre rôle / état

    return (
        <>
            <div className="page-head">
                <h1>Utilisateurs</h1>
                <span className="muted">{nbResultats} utilisateur{nbResultats > 1 ? "s" : ""}</span>
            </div>

            {/* compteurs par rôle */}
            {compteurs && (
                <div className="stats">
                    {ROLES.map((r, i) => (
                        <div key={r} className="stat">
                            <b>{compteurs.parRole[i]}</b>
                            <span>{LIBELLE_ROLE[r]}s</span>
                        </div>
                    ))}
                </div>
            )}

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
                    <button type="button" aria-pressed={filtreRole === ""} onClick={() => choisirRole("")}>
                        Tous les rôles
                    </button>
                    {ROLES.map((r) => (
                        <button key={r} type="button" aria-pressed={filtreRole === r}
                                onClick={() => choisirRole(r)}>{LIBELLE_ROLE[r]}</button>
                    ))}
                </div>
                <div className="seg" role="group" aria-label="État du compte">
                    <button type="button" aria-pressed={filtreEtat === ""} onClick={() => choisirEtat("")}>
                        Actifs et inactifs
                    </button>
                    <button type="button" aria-pressed={filtreEtat === "actifs"} onClick={() => choisirEtat("actifs")}>
                        Actifs
                    </button>
                    <button type="button" aria-pressed={filtreEtat === "inactifs"} onClick={() => choisirEtat("inactifs")}>
                        Inactifs{compteurs && ` (${compteurs.inactifs})`}
                    </button>
                    <button type="button" aria-pressed={filtreEtat === "verrouilles"} onClick={() => choisirEtat("verrouilles")}>
                        Verrouillés{compteurs && ` (${compteurs.verrouilles})`}
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
                {!resultat && !erreurChargement && <p className="muted">Chargement des utilisateurs…</p>}
                {resultat && nbResultats === 0 && <p className="muted">Aucun utilisateur ne correspond.</p>}
            </div>

            {/* boutons de changement de page */}
            {resultat && resultat.totalPages > 1 && (
                <nav className="pagination" aria-label="Pages">
                    <button type="button" className="btn sm" onClick={() => setPage((p) => p - 1)}
                            disabled={resultat.first}>
                        ‹ Précédent
                    </button>
                    <span className="muted small">
                        Page {resultat.page + 1} sur {resultat.totalPages} ({nbResultats} utilisateurs)
                    </span>
                    <button type="button" className="btn sm" onClick={() => setPage((p) => p + 1)}
                            disabled={resultat.last}>
                        Suivant ›
                    </button>
                </nav>
            )}

            {/* création / modification : le composant n'existe que pendant que la popup est ouverte */}
            {(popup?.type === "creer" || popup?.type === "modifier") && (
                <UtilisateurPopup
                    key={aModifier?.id ?? "creation"}
                    utilisateur={aModifier}
                    estMoi={estMoi}
                    onClose={fermerPopup}
                    onEnregistre={enregistre}
                />
            )}

            {/* suppression : une simple confirmation, gardée dans la page */}
            {aSupprimer && (
                <Popup
                    open
                    onClose={fermerPopup}
                    title={`Supprimer ${aSupprimer.prenom} ${aSupprimer.nom} ?`}
                    description={`Le compte ${aSupprimer.login} sera supprimé définitivement. Pour bloquer l’accès sans supprimer, désactivez-le ou verrouillez-le.`}
                    confirmLabel="Supprimer l’utilisateur"
                    variant="danger"
                    onConfirm={() => handleSupprimer(aSupprimer)}
                    loading={envoi}
                >
                    {erreur && <p className="error-message" role="alert">{erreur}</p>}
                </Popup>
            )}
        </>
    );
}