import {useEffect, useState} from "react";
import {
    LIBELLE_ETAT_MATERIEL,
    LIBELLE_TYPE_MATERIEL,
    type EtatMateriel,
    type Materiel,
    type TypeMateriel, type Page,
} from "../types/types.ts";
import {compterMateriels, getMateriels} from "../services/materielService.ts";
import {
    LIBELLE_LOCALISATION,
    ORDRE_ETATS,
    ORDRE_TYPES,
    type Localisation, versRsql, NOM_TYPE,
} from "../utils/materiels.ts";
import AjouterMaterielPopup from "../components/AjouterMaterielPopup.tsx";
import MaterielLigne from "../components/MaterielLigne.tsx";


const TAILLE_PAGE = 20;

interface Compteurs {
    total: number;      // tout le matériel en service
    archives: number;
    stock: number[];    // en stock, dans l'ordre de ORDRE_TYPES pour l'affichage du stock
}

const LOCALISATIONS: Localisation[] = ["PARTOUT", "SALLE", "STOCK", "ARCHIVE"];

// Les compteurs sont indépendants des filtres
async function chargerCompteurs(): Promise<Compteurs> {
    const [total, archives, ...stock] = await Promise.all([
        compterMateriels({localisation: "PARTOUT"}),
        compterMateriels({localisation: "ARCHIVE"}),
        ...ORDRE_TYPES.map((t) => compterMateriels({localisation: "STOCK", type: t})),
    ]);
    return {total, archives, stock};
}


export default function MaterielPage() {
    // filtres
    const [saisie, setSaisie] = useState("");         // ce qui est tapé dans le champ
    const [recherche, setRecherche] = useState("");   // ce qui est envoyé au back
    const [type, setType] = useState<TypeMateriel | null>(null);
    const [etat, setEtat] = useState<EtatMateriel | null>(null);
    const [localisation, setLocalisation] = useState<Localisation>("PARTOUT");
    const [page, setPage] = useState(0);

    // données
    const [resultat, setResultat] = useState<Page<Materiel>>();
    const [compteurs, setCompteurs] = useState<Compteurs>();
    const [erreur, setErreur] = useState("");
    const [ajoutOuvert, setAjoutOuvert] = useState(false);
    const [version, setVersion] = useState(0); // +1 = recharger la liste et les compteurs

    // la recherche part 500 ms après la dernière frappe pour pas faire de requête à chaque saisie
    useEffect(() => {
        const minuteur = setTimeout(() => {
            setRecherche(saisie);
            setPage(0);
        }, 500);
        return () => clearTimeout(minuteur);
    }, [saisie]);

    // recharge la page à chaque changement de filtre ou de page
    useEffect(() => {
        let actif = true; // pour ignorer une réponse qui arrive après un chagement de filtre
        getMateriels({filter: versRsql(recherche), type, etat, localisation}, page, TAILLE_PAGE)
            .then((p) => {
                if (actif) {
                    setResultat(p);
                    setErreur("");
                }
            })
            .catch(() => {
                if (actif) setErreur("Impossible de charger le matériel.");
            });
        return () => { actif = false; };
    }, [recherche, type, etat, localisation, page, version]);

    // Les compteurs ne changent pas avec les filtres, seulement quand du matériel est ajouté
    useEffect(() => {
        chargerCompteurs().then(setCompteurs).catch(() => {});
    }, [version]);

    // changer un filtre ramène toujours à la première pagination
    function changerType(t: TypeMateriel | null) { setType(t); setPage(0); }
    function changerEtat(e: EtatMateriel | null) { setEtat(e); setPage(0); }
    function changerLocalisation(l: Localisation) { setLocalisation(l); setPage(0); }

    const recharger = () => setVersion(v => v + 1);

    return (
        <>
            <div className="page-head">
                <h1>Matériel</h1>
                {resultat && compteurs && (
                    <span className="muted">{resultat.totalElements} sur {compteurs.total} en service</span>
                )}
            </div>

            {/* matériel en stock, par type */}
            {compteurs && (
                <div className="stats">
                    {ORDRE_TYPES.map((t, i) => (
                        <div key={t} className="stat">
                            <b>{compteurs.stock[i]}</b>
                            <span>{NOM_TYPE[t][compteurs.stock[i] > 1 ? 1 : 0]} en stock</span>
                        </div>
                    ))}
                </div>
            )}

            <div className="hero-row">
                <button type="button" className="btn primary" onClick={() => setAjoutOuvert(true)}>
                    Ajouter un matériel
                </button>
                {/* export : plus tard */}
                <button type="button" className="btn" disabled title="Bientôt disponible">
                    Exporter l'inventaire
                </button>
            </div>

            <input
                className="search"
                type="search"
                placeholder="Rechercher un matériel : modèle, n° de série"
                aria-label="Rechercher un matériel"
                value={saisie}
                onChange={(e) => setSaisie(e.target.value)}
            />

            <div className="filters">
                <div className="seg" role="group" aria-label="Type">
                    <button type="button" aria-pressed={type === null} onClick={() => changerType(null)}>
                        Tous les types
                    </button>
                    {ORDRE_TYPES.map((t) => (
                        <button key={t} type="button" aria-pressed={type === t} onClick={() => changerType(t)}>
                            {LIBELLE_TYPE_MATERIEL[t]}
                        </button>
                    ))}
                </div>

                <div className="seg" role="group" aria-label="État">
                    <button type="button" aria-pressed={etat === null} onClick={() => changerEtat(null)}>
                        Tous les états
                    </button>
                    {ORDRE_ETATS.map((e) => (
                        <button key={e} type="button" aria-pressed={etat === e} onClick={() => changerEtat(e)}>
                            {LIBELLE_ETAT_MATERIEL[e]}
                        </button>
                    ))}
                </div>

                <div className="seg" role="group" aria-label="Emplacement">
                    {LOCALISATIONS.map((l) => (
                        <button key={l} type="button" aria-pressed={localisation === l}
                                onClick={() => changerLocalisation(l)}>
                            {LIBELLE_LOCALISATION[l]}
                            {l === "ARCHIVE" && compteurs && ` (${compteurs.archives})`}
                        </button>
                    ))}
                </div>
            </div>

            {erreur && <p className="error-message" role="alert">{erreur}</p>}
            {!resultat && !erreur && <p className="muted">Chargement du matériel…</p>}
            {resultat && resultat.totalElements === 0 && (
                <p className="muted">Aucun matériel ne correspond à ces filtres.</p>
            )}

            {resultat && resultat.content.length > 0 && (
                <div className="list">
                    {resultat.content.map((m) => (
                        <MaterielLigne key={m.id} materiel={m} onChange={recharger}/>
                    ))}
                </div>
            )}

            {/* boutons de changement de page */}
            {resultat && resultat.totalPages > 1 && (
                <nav className="pagination" aria-label="Pages">
                    <button type="button" className="btn sm" disabled={resultat.first}
                            onClick={() => setPage((p) => p - 1)}>
                        ‹ Précédent
                    </button>
                    <span className="muted small">
                        Page {resultat.page + 1} sur {resultat.totalPages} ({resultat.totalElements} matériels)
                    </span>
                    <button type="button" className="btn sm" disabled={resultat.last}
                            onClick={() => setPage((p) => p + 1)}>
                        Suivant ›
                    </button>
                </nav>
            )}

            {ajoutOuvert && (
                <AjouterMaterielPopup
                    onClose={() => setAjoutOuvert(false)}
                    onFait={() => {
                        setAjoutOuvert(false);
                        recharger();
                    }}/>
            )}
        </>
    );
}