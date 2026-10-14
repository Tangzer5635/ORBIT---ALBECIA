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
import {pluriel} from "../utils/postes.ts";


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
    }, [recherche, type, etat, localisation, page]);

    // Les compteurs n'ont pas besoin de changer au changement de filtre, c'est le matériel en stock
    //TODO le changer quand on pourra affecter ou supprimer
    useEffect(() => {
        chargerCompteurs().then(setCompteurs).catch(() => {});
    }, []);

    // changer un filtre ramène toujours à la première pagination
    function changerType(t: TypeMateriel | null) { setType(t); setPage(0); }
    function changerEtat(e: EtatMateriel | null) { setEtat(e); setPage(0); }
    function changerLocalisation(l: Localisation) { setLocalisation(l); setPage(0); }

    return (
        <>
            <h1>Matériel</h1>
            {resultat && compteurs && <p>{resultat.totalElements} sur {compteurs.total}</p>}

            <button type="button" className="btn primary">
                Ajouter un matériel
            </button>
            <button type="button">
                Exporter l'inventaire
            </button>

            {compteurs && (
                <div>
                    <span>En stock</span>
                    <ul>
                        {ORDRE_TYPES.map((t, i) => (
                            <li key={t}>{pluriel(compteurs.stock[i], NOM_TYPE[t][0], NOM_TYPE[t][1])}</li>
                        ))}
                    </ul>
                </div>
            )}

            <div role="search">
                <input
                    type="search"
                    placeholder="Rechercher un matériel : modèle, n° de série"
                    value={saisie}
                    onChange={(e) => setSaisie(e.target.value)}
                />

                <div>
                    <button type="button" aria-pressed={type === null} onClick={() => changerType(null)}>
                        Tous les types
                    </button>
                    {ORDRE_TYPES.map((t) => (
                        <button key={t} type="button" aria-pressed={type === t} onClick={() => changerType(t)}>
                            {LIBELLE_TYPE_MATERIEL[t]}
                        </button>
                    ))}
                </div>

                <div>
                    <div>
                        <button type="button" aria-pressed={etat === null} onClick={() => changerEtat(null)}>
                            Tous les états
                        </button>
                        {ORDRE_ETATS.map((e) => (
                            <button key={e} type="button" aria-pressed={etat === e} onClick={() => changerEtat(e)}>
                                {LIBELLE_ETAT_MATERIEL[e]}
                            </button>
                        ))}
                    </div>

                    <div>
                        {LOCALISATIONS.map((l) => (
                            <button key={l} type="button" aria-pressed={localisation === l}
                                    onClick={() => changerLocalisation(l)}>
                                {LIBELLE_LOCALISATION[l]}
                                {l === "ARCHIVE" && compteurs && ` (${compteurs.archives})`}
                            </button>
                        ))}
                    </div>
                </div>
            </div>

            {erreur && <p role="alert">{erreur}</p>}
            {!resultat && !erreur && <p>Chargement du matériel…</p>}
            {resultat && resultat.totalElements === 0 && <p>Aucun matériel ne correspond à ces filtres.</p>}

            {resultat && resultat.content.length > 0 && (
                <ul>
                    {resultat.content.map((m) => (
                        <li key={m.id}>
                            {LIBELLE_TYPE_MATERIEL[m.type]} - {m.modele} - {LIBELLE_ETAT_MATERIEL[m.etat]}
                            {" "}- garantie jusqu'au {new Date(m.dateFinGarantie).toLocaleDateString("fr-FR")}
                        </li>
                    ))}
                </ul>
            )}

            {/*Affichage des boutons de changement de page
            https://developer.mozilla.org/fr/docs/Web/Accessibility/ARIA/Reference/Roles/navigation_role */}
            {resultat && resultat.totalPages > 1 && (
                <nav aria-label="Pagination">
                    <button type="button" disabled={resultat.first} onClick={() => setPage((p) => p - 1)}>
                        Précédent
                    </button>
                    <span>Page {resultat.page + 1} sur {resultat.totalPages}</span>
                    <button type="button" disabled={resultat.last} onClick={() => setPage((p) => p + 1)}>
                        Suivant
                    </button>
                </nav>
            )}
        </>
    );
}