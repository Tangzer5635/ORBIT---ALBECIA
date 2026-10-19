import {type Formateur, LIBELLE_TYPE_FORMATION, type Salle} from "../../types/types.ts";
import {dateFr} from "../../utils/postes.ts";

type Props = {
    salle: Salle;
    // actions absentes pour qui ne gère pas la salle (formateur)
    onAffecterStage?: () => void;
    onRetirerStage?: () => void;
    onAffecterFormateur?: () => void;
    onRetirerFormateur?: (formateur: Formateur) => void;
};

// Panneau de droite quand rien n'est sélectionné sur le plan : stage, gestionnaire, formateurs et master de la salle
export default function PanneauSalle({
                                         salle, onAffecterStage, onRetirerStage, onAffecterFormateur, onRetirerFormateur
                                     }: Props) {
    const formateurs = salle.formateurs ?? [];

    return (
        <aside className="poste-panneau" aria-label="Vue de la salle">
            <div>
                <h2>Vue de la salle</h2>
                <p className="muted small">Choisis un poste ou un matériel sur le plan pour afficher son détail ici.</p>
            </div>

            <section className="panneau-section">
                <h3>Stage</h3>
                {salle.stageId ? (
                    <>
                        <b>{salle.stageNom}</b>
                        <p className="muted small">
                            {salle.stageType && `${LIBELLE_TYPE_FORMATION[salle.stageType]}, `}
                            {salle.stageDateDebut && salle.stageDateFin &&
                                `du ${dateFr(salle.stageDateDebut)} au ${dateFr(salle.stageDateFin)}, `}
                            {salle.stageFormateur && `géré par ${salle.stageFormateur}`}
                        </p>
                        {onRetirerStage &&
                            <button type="button" className="btn sm" onClick={onRetirerStage}>Retirer le stage</button>}
                    </>
                ) : (
                    <>
                        <p className="muted">Aucun stage dans cette salle.</p>
                        {onAffecterStage &&
                            <button type="button" className="btn sm" onClick={onAffecterStage}>Affecter un
                                stage</button>}
                    </>
                )}
            </section>

            <section className="panneau-section">
                <h3>Gestionnaire</h3>
                {salle.creePar
                    ? <p>{salle.creePar} <span className="muted small">(a créé la salle)</span></p>
                    : <p className="muted">Non renseigné</p>}
            </section>

            <section className="panneau-section">
                <h3>Formateurs attribués</h3>
                {formateurs.length === 0 ? (
                    <p className="muted">Aucun formateur attribué.</p>
                ) : (
                    <ul className="panneau-liste">
                        {formateurs.map(f => (
                            <li key={f.id}>
                                {f.prenom} {f.nom}
                                {onRetirerFormateur && (
                                    <button type="button" className="muted small lien"
                                            onClick={() => onRetirerFormateur(f)}>Retirer</button>
                                )}
                            </li>
                        ))}
                    </ul>
                )}
                {onAffecterFormateur &&
                    <button type="button" className="btn sm" onClick={onAffecterFormateur}>Attribuer un
                        formateur</button>}
            </section>

            <section className="panneau-section">
                <h3>Master</h3>
                {salle.masterNom
                    ? <b>{salle.masterNom}</b>
                    : <p className="muted">Aucun master plaqué sur cette salle.</p>}
            </section>
        </aside>
    );
}