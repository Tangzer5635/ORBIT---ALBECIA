import {Fragment, useEffect, useState} from "react";
import Popup from "../Popup.tsx";
import {LIBELLE_TYPE_MATERIEL, type MaterielStock, type TypeMateriel} from "../../../types/types.ts";
import {affecterMaterielAPoste, getStock, MaterielError} from "../../../services/materielService.ts";
import {MAX_PAR_POSTE, NOM_TYPE, ORDRE_TYPES, placeSurPoste} from "../../../utils/materiels.ts";

interface Props {
    posteId: number;
    titrePoste: string; // "Poste 04", "Poste formateur"
    numBatiment?: string | null; // bâtiment de la salle : son stock est affiché en premier
    materielsParType: Partial<Record<TypeMateriel, number>>; // ce que le poste a déjà (1 UC et 2 écrans au plus)
    onClose: () => void;
    onFait: () => void; // le poste a changé : la page se recharge
}

// regroupe par bâtiment : celui de la salle d'abord, puis les autres par numéro
function parBatiment(materiels: MaterielStock[], premier?: string | null): [string, MaterielStock[]][] {
    const groupes = new Map<string, MaterielStock[]>();
    for (const m of materiels) {
        groupes.set(m.numBatiment, [...(groupes.get(m.numBatiment) ?? []), m]);
    }
    return [...groupes].sort(([a], [b]) =>
        Number(b === premier) - Number(a === premier) || a.localeCompare(b, "fr", {numeric: true}));
}

export default function AffecterMaterielPopup({posteId, titrePoste, numBatiment, materielsParType, onClose, onFait}: Props) {
    // seuls les types que le poste peut encore recevoir (jamais de vidéoprojecteur ni de tableau)
    const typesPossibles = ORDRE_TYPES.filter(t => placeSurPoste(materielsParType, t));
    const [stock, setStock] = useState<MaterielStock[] | null>(null);
    const [recherche, setRecherche] = useState("");
    const [type, setType] = useState<TypeMateriel | null>(null);
    const [choix, setChoix] = useState<number | null>(null);
    const [erreur, setErreur] = useState("");
    const [enCours, setEnCours] = useState(false);

    // seul le matériel en stock peut être affecté
    useEffect(() => {
        getStock()
            .then(setStock)
            .catch(() => setErreur("Impossible de charger le stock."));
    }, []);

    // chaque mot tapé doit se trouver dans le modèle, le n° de série ou le nom de machine
    const mots = recherche.toLowerCase().split(/\s+/).filter(Boolean);
    const visibles = (stock ?? [])
        .filter(m => typesPossibles.includes(m.type))
        .filter(m => !type || m.type === type)
        .filter(m => {
            const texte = `${m.modele} ${m.numSerie} ${m.nomMachine ?? ""}`.toLowerCase();
            return mots.every(mot => texte.includes(mot));
        })
        // dans chaque bâtiment : par type (UC, écran...) puis par modèle
        .sort((a, b) => ORDRE_TYPES.indexOf(a.type) - ORDRE_TYPES.indexOf(b.type) || a.modele.localeCompare(b.modele, "fr"));

    // un matériel caché par les filtres n'est plus choisi
    const choisi = visibles.some(m => m.id === choix) ? choix : null;

    async function valider() {
        if (!choisi) return;
        setEnCours(true);
        setErreur("");
        try {
            await affecterMaterielAPoste(choisi, posteId);
            onFait();
        } catch (err) {
            setErreur(err instanceof MaterielError && err.detail ? err.detail : "Affectation impossible.");
            setEnCours(false);
        }
    }

    return (
        <Popup
            open
            onClose={onClose}
            title={`Affecter un matériel : ${titrePoste}`}
            confirmLabel="Affecter"
            onConfirm={valider}
            loading={enCours}
            disabled={!choisi}
        >
            {typesPossibles.length === 0 ? (
                <p className="muted">
                    Le poste est complet : {MAX_PAR_POSTE.UC} {NOM_TYPE.UC[0]} et {MAX_PAR_POSTE.Ecran} {NOM_TYPE.Ecran[1]} au maximum.
                </p>
            ) : <>
            {stock === null && !erreur && <p className="muted">Chargement du stock…</p>}
            {stock !== null && stock.length === 0 && <p className="muted">Aucun matériel en stock.</p>}

            {stock !== null && stock.length > 0 && (
                <>
                    <div className="filters">
                        <div className="seg" role="group" aria-label="Type">
                            <button type="button" aria-pressed={type === null} onClick={() => setType(null)}>
                                Tous les types
                            </button>
                            {typesPossibles.map(t => (
                                <button key={t} type="button" aria-pressed={type === t} onClick={() => setType(t)}>
                                    {LIBELLE_TYPE_MATERIEL[t]}
                                </button>
                            ))}
                        </div>
                    </div>

                    <label htmlFor="rechercheMateriel">Matériel en stock ({visibles.length})</label>
                    <input
                        id="rechercheMateriel"
                        type="search"
                        placeholder="Rechercher : modèle, n° de série, nom de machine"
                        value={recherche}
                        onChange={(e) => setRecherche(e.target.value)}
                        onKeyDown={(e) => e.key === "Enter" && e.preventDefault()} // Entrée ne doit pas affecter
                        autoFocus
                    />

                    {/* même liste que « Gérer les stagiaires », un titre par bâtiment */}
                    <div className="stagiaires-liste">
                        {parBatiment(visibles, numBatiment).map(([batiment, materiels]) => (
                            <Fragment key={batiment}>
                                <b className="small">
                                    Bâtiment {batiment}
                                    {batiment === numBatiment && " (bâtiment de la salle)"}
                                </b>
                                {materiels.map(m => (
                                    <label key={m.id} className="stagiaire-ligne">
                                        <input type="radio" name="materiel" checked={m.id === choisi}
                                               onChange={() => setChoix(m.id)}/>
                                        <span>
                                            <b>{m.modele}</b>
                                            <small>
                                                {LIBELLE_TYPE_MATERIEL[m.type]}, {m.numSerie}
                                                {m.nomMachine && `, ${m.nomMachine}`}, réserve {m.numSalle}
                                            </small>
                                        </span>
                                    </label>
                                ))}
                            </Fragment>
                        ))}
                        {visibles.length === 0 && <p className="muted">Aucun matériel ne correspond.</p>}
                    </div>
                </>
            )}
            </>}
            {erreur && <p className="error-message" role="alert">{erreur}</p>}
        </Popup>
    );
}