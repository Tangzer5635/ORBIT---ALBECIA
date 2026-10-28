import {LIBELLE_ETAT_MATERIEL, LIBELLE_TYPE_MATERIEL, type Materiel, type Poste, type Salle} from "../../types/types.ts";
import MaterielCard from "./MaterielCard.tsx";
import ListePostes from "../Poste/ListePostes.tsx";
import {CLASSE_ETAT_MATERIEL} from "../../utils/materiels.ts";

type Props = {
    materiel: Materiel;
    salle: Salle;
    postes: Poste[]; // tous les postes de la salle (liste de droite)
    onChoisirPoste: (id: number) => void;
    onFermer: () => void; // retour à la vue de la salle
    onChange: () => void; // le matériel a bougé : la page se recharge
};

// Matériel posé dans la salle (vidéoprojecteur, tableau interactif…), sur 2 colonnes de la grille .plan-corps :
// sa fiche au centre, la liste des postes à droite
export default function PanneauMateriel({materiel, salle, postes, onChoisirPoste, onFermer, onChange}: Props) {
    return (
        <>
            <div className="poste-centre" aria-label={LIBELLE_TYPE_MATERIEL[materiel.type]}>
                <div className={`panneau-entete ${CLASSE_ETAT_MATERIEL[materiel.etat]}`}>
                    <div className="poste-entete">
                        <h2>{LIBELLE_TYPE_MATERIEL[materiel.type]}</h2>
                        <span className="pastille">{LIBELLE_ETAT_MATERIEL[materiel.etat]}</span>
                    </div>
                    <p className="muted small">Matériel de salle, salle {salle.numSalle}</p>
                </div>

                <div className="centre-mats">
                    <MaterielCard materiel={materiel} depuis={{salleId: salle.id}} onChange={onChange}/>
                </div>
            </div>

            <aside className="poste-panneau">
                <button type="button" className="btn vue-salle" onClick={onFermer}>‹ Vue de la salle</button>
                <ListePostes postes={postes} posteId={null} onChoisir={onChoisirPoste}/>
            </aside>
        </>
    );
}