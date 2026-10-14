import {useEffect, useMemo, useState} from "react";
import {
    LIBELLE_ETAT_MATERIEL,
    LIBELLE_TYPE_MATERIEL,
    type EtatMateriel,
    type Materiel,
    type TypeMateriel,
} from "../types/types.ts";
import {getMateriel} from "../services/materielService.ts";
import {
    filtrerMateriels,
    LIBELLE_LOCALISATION,
    ORDRE_ETATS,
    ORDRE_TYPES,
    type Localisation, compterStock,
} from "../utils/materiels.ts";
import {pluriel} from "../utils/postes.ts";

const LOCALISATIONS: Localisation[] = ["PARTOUT", "SALLE", "STOCK", "ARCHIVE"];

export default function MaterielPage() {
    const [materiels, setMateriels] = useState<Materiel[]>();
    const [erreur, setErreur] = useState("");

    // un état par filtre
    const [recherche, setRecherche] = useState("");
    const [type, setType] = useState<TypeMateriel | null>(null);
    const [etat, setEtat] = useState<EtatMateriel | null>(null);
    const [localisation, setLocalisation] = useState<Localisation>("PARTOUT");

    const uc = compterStock(materiels ?? [], "UC")
    const ecran = compterStock(materiels ?? [], "Ecran")
    const vp = compterStock(materiels ?? [], "VideoProjecteur")
    const ti = compterStock(materiels ?? [], "TableauInteractif")

    useEffect(() => {
        getMateriel()
            .then(setMateriels)
            .catch(() => setErreur("Impossible de charger le matériel."));
    }, []);

    const resultats = useMemo(
        () => filtrerMateriels(materiels ?? [], {recherche, type, etat, localisation}, (t) => LIBELLE_TYPE_MATERIEL[t]),
        [materiels, recherche, type, etat, localisation]
    );
    const nbArchives = useMemo(
        () => (materiels ?? []).filter((m) => m.etat === "ARCHIVE").length,
        [materiels]
    );

    return (
        <>
            <h1>Matériel</h1>
            <p>{resultats.length} sur {materiels?.length}</p>

            <button type="button" className="btn primary">
                Ajouter un matériel
            </button>
            <button type="button">
                Exporter l'inventaire
            </button>

            <div>
                <p>En stock {pluriel(uc,"unité centrale")} {pluriel(ecran, "écran")} {pluriel(vp, "vidéoprojecteur")} {pluriel(ti, "tableau interactif")}</p>
            </div>

            <div role="search">
                <input
                    type="search"
                    className="filtres-recherche"
                    placeholder="Rechercher un matériel : numéro, type, modèle, n° de série, salle"
                    aria-label="Rechercher un matériel"
                    value={recherche}
                    onChange={(e) => setRecherche(e.target.value)}
                />

                <div>
                    <button type="button" aria-pressed={type === null} onClick={() => setType(null)}>
                        Tous les types
                    </button>
                    {ORDRE_TYPES.map((t) => (
                        <button key={t} type="button" aria-pressed={type === t} onClick={() => setType(t)}>
                            {LIBELLE_TYPE_MATERIEL[t]}
                        </button>
                    ))}
                </div>

                <div>
                    <div>
                        <button type="button" aria-pressed={etat === null} onClick={() => setEtat(null)}>
                            Tous les états
                        </button>
                        {ORDRE_ETATS.map((e) => (
                            <button key={e} type="button" aria-pressed={etat === e} onClick={() => setEtat(e)}>
                                {LIBELLE_ETAT_MATERIEL[e]}
                            </button>
                        ))}
                    </div>

                    <div>
                        {LOCALISATIONS.map((l) => (
                            <button key={l} type="button" aria-pressed={localisation === l}
                                    onClick={() => setLocalisation(l)}>
                                {LIBELLE_LOCALISATION[l]}
                                {l === "ARCHIVE" && ` (${nbArchives})`}
                            </button>
                        ))}
                    </div>
                </div>
            </div>

            {erreur && <p>{erreur}</p>}
            {!materiels && !erreur && <p>Chargement du matériel…</p>}
            {materiels && resultats.length === 0 && <p>Aucun matériel ne correspond à ces filtres.</p>}


            {resultats.length>0 && (
                <div>
                    {resultats.map((m) => (
                        <div>
                            {LIBELLE_TYPE_MATERIEL[m.type]}{m.nomMachine && ` (${m.nomMachine})`} - {m.modele} -- {LIBELLE_ETAT_MATERIEL[m.etat]}{" "} - {new Date(m.dateFinGarantie).toLocaleDateString("fr-FR")}
                        </div>
                    ))}
                </div>
            )}
        </>
    );
}