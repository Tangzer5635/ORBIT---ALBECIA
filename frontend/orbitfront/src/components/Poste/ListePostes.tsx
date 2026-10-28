import type {Poste} from "../../types/types.ts";
import {estFormateur, etatPoste, nomPoste, pluriel} from "../../utils/postes.ts";

type Props = {
    postes: Poste[];          // poste formateur en premier, puis les postes stagiaires dans l'ordre
    posteId: number | null;   // poste affiché au centre (null : un matériel de salle est affiché)
    onChoisir: (id: number) => void;
};

// Colonne de droite quand un poste ou un matériel de salle est affiché au centre :
// liste des postes pour passer de l'un à l'autre (sous les actions du poste)
export default function ListePostes({postes, posteId, onChoisir}: Props) {
    return (
        <section className="panneau-section">
            <h3>
                Postes de la salle <span className="muted">({postes.length})</span>
            </h3>

            <div className="liste-postes">
                {postes.map(p => {
                    const etat = etatPoste(p.etat);
                    const occupant = estFormateur(p) ? "Formateur" : p.occupant ?? "Aucun stagiaire";
                    return (
                        <button key={p.id} type="button"
                                className={`liste-poste ${etat?.classe ?? ""}${p.id === posteId ? " select" : ""}`}
                                aria-pressed={p.id === posteId}
                                onClick={() => onChoisir(p.id)}>
                            <span className="liste-poste-ligne">
                                <strong>{nomPoste(p)}</strong>
                                {etat && <span className="pastille">{etat.libelle}</span>}
                            </span>
                            <small>{occupant} · {pluriel(p.materielsId.length, "matériel")}</small>
                        </button>
                    );
                })}
            </div>
        </section>
    );
}