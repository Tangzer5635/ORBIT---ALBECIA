import {useEffect, useState} from "react";
import {LIBELLE_ROLE, type Page, type Role, type Utilisateur} from "../types/types.ts";
import {changerVerrou, changerActif, compterUtilisateurs, type FiltresUtilisateur, getUtilisateurs, UtilisateurError,} from "../services/utilisateurService.ts";
import {getUtilisateur} from "../services/authService.ts";
import UtilisateurLigne from "../components/Utilisateur/UtilisateurLigne.tsx";
import {rechercheVersRsql} from "../utils/utilisateur.ts";
import Pagination from "../components/Utils/Pagination.tsx";
import ModifierUtilisateurPopup, {type PopupState,} from "../components/Popup/Personnel/ModifierUtilisateurPopup.tsx";

const ROLES = Object.keys(LIBELLE_ROLE) as Role[];
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
async function chargerCompteurs(): Promise<Compteurs> {
    const [inactifs, verrouilles, ...parRole] = await Promise.all([
        compterUtilisateurs({active: false}),
        compterUtilisateurs({verrouille: true}),
        ...ROLES.map((r) => compterUtilisateurs({role: r})),
    ]);
    return {parRole, inactifs, verrouilles};
}

export default function UtilisateurPage() {
    // données renvoyées par le back
    const [resultat, setResultat] = useState<Page<Utilisateur>>();
    const [compteurs, setCompteurs] = useState<Compteurs>();
    const [erreurChargement, setErreurChargement] = useState<string>();
    const [error, setError] = useState<string>();
    const [version, setVersion] = useState(0);
    const moi = getUtilisateur();

    const [popup, setPopup] = useState<PopupState>(null);

    // filtres
    const [saisie, setSaisie] = useState("");
    const [recherche, setRecherche] = useState("");
    const [filtreRole, setFiltreRole] = useState<Role | null>(null);
    const [filtreEtat, setFiltreEtat] = useState<EtatCompte | null>(null);
    const [page, setPage] = useState(0);

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
        let actif = true;
        const etat = ETATS.find((e) => e.cle === filtreEtat)?.filtres ?? {};
        getUtilisateurs({filter: rechercheVersRsql(recherche), role: filtreRole, ...etat}, page, TAILLE_PAGE)
            .then((p) => {
                if (!actif) return;
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
    async function handleActif(u: Utilisateur) {
        setError(undefined);
        try {
            await changerActif(u.id, !u.active);
            recharger();
        } catch (err) {
            if (err instanceof UtilisateurError && err.status === 400) setError(err.detail || "Action impossible sur ce compte.");
            else if (err instanceof UtilisateurError && err.status === 403) setError("Vous n'avez pas le droit d'activer ou de désactiver un compte.");
            else setError("Le service est indisponible. Réessayez plus tard.");
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

    // un changement de filtre ramène à la première page
    function changerRole(r: Role | null) {
        setFiltreRole(r);
        setPage(0);
    }

    function changerEtat(e: EtatCompte | null) {
        setFiltreEtat(e);
        setPage(0);
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

            <button type="button" className="btn primary" onClick={() => setPopup({type: "creer"})}>Créer un utilisateur</button>

            {(error ?? erreurChargement) && <p className="error-message" role="alert">{error ?? erreurChargement}</p>}

            <input className="search" type="search" placeholder="Rechercher un utilisateur par nom ou identifiant" aria-label="Rechercher un utilisateur" value={saisie} onChange={(e) => setSaisie(e.target.value)}/>

            <div className="filters">
                <div className="seg" role="group" aria-label="Rôle">
                    <button type="button" aria-pressed={filtreRole === null} onClick={() => changerRole(null)}>Tous les rôles</button>
                    {ROLES.map((r) => (
                        <button key={r} type="button" aria-pressed={filtreRole === r} onClick={() => changerRole(r)}>{LIBELLE_ROLE[r]}</button>
                    ))}
                </div>
                <div className="seg" role="group" aria-label="État du compte">
                    <button type="button" aria-pressed={filtreEtat === null} onClick={() => changerEtat(null)}>Actifs et inactifs</button>
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
                    <UtilisateurLigne key={u.id} utilisateur={u} estMoi={u.id === moi?.id} onModifier={(utilisateur) => setPopup({type: "modifier", utilisateur})} onVerrou={handleVerrou} onActif={handleActif} onSupprime={(utilisateur) => setPopup({type: "supprimer", utilisateur})}/>
                ))}
                {!resultat && !erreurChargement && <p className="muted">Chargement…</p>}
                {resultat && resultat.totalElements === 0 && <p className="muted">Aucun utilisateur ne correspond.</p>}
            </div>

            {/* boutons de changement de page (même principe que MaterielPage) */}
            {resultat && <Pagination page={resultat.page} totalPages={resultat.totalPages} onChange={setPage} info={`(${resultat.totalElements} utilisateurs)`}/>}

            <ModifierUtilisateurPopup popup={popup} onClose={() => setPopup(null)} onSuccess={recharger}/>
        </>
    );
}