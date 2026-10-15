import {LIBELLE_ETAT_MATERIEL, LIBELLE_TYPE_MATERIEL, type Materiel, type Salle} from "../../types/types.ts";
import MaterielCard from "./MaterielCard.tsx";
import {CLASSE_ETAT_MATERIEL} from "../../utils/materiels.ts";

type Props = {
    materiel: Materiel;
    salle: Salle;
    onFermer: () => void;
    onChange: () => void; // le matériel a bougé : la page se recharge
};

// Matériel posé dans la salle (vidéoprojecteur, tableau interactif…)
export default function PanneauMateriel({materiel, salle, onFermer, onChange}: Props) {
    return (
        <aside className="poste-panneau" aria-label={LIBELLE_TYPE_MATERIEL[materiel.type]}>
            <button type="button" className="muted small lien" onClick={onFermer}>‹ Vue de la salle</button>

            <div className={`panneau-entete ${CLASSE_ETAT_MATERIEL[materiel.etat]}`}>
                <div className="poste-entete">
                    <h2>{LIBELLE_TYPE_MATERIEL[materiel.type]}</h2>
                    <span className="pastille">{LIBELLE_ETAT_MATERIEL[materiel.etat]}</span>
                </div>
                <p className="muted small">Matériel de salle, salle {salle.numSalle}</p>
            </div>

            <MaterielCard materiel={materiel} depuis={{salleId: salle.id}} onChange={onChange}/>
        </aside>
    );
}