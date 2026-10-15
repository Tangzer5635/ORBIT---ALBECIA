import type {EtatPoste, Formateur, Poste, Salle} from "../types/types.ts";

type Props = {
    poste: Poste;
    salle: Salle;
    numero: number;
    onFermer: () => void;
    onAffecterStagiaire: () => void;
    formateur?: boolean; // poste formateur de la salle : affecté aux formateurs de la salle
    onAffecterFormateur?: () => void; // seulement pour qui gère la salle
    onRetirerFormateur?: (formateur: Formateur) => void;
};

const LIBELLE_ETAT: Record<EtatPoste, string> = {
    DEFECTUEUX: "Défectueux",
    A_COMPLETER: "À compléter",
    A_CHANGER: "Prévoir changement",
    UTILISE: "Utilisé",
    DISPONIBLE: "Disponible",
};

export default function PanneauPoste({
                                         poste, salle, numero, onFermer, onAffecterStagiaire,
                                         formateur, onAffecterFormateur, onRetirerFormateur
                                     }: Props) {
    if (formateur) {
        const formateurs = salle.formateurs ?? [];
        return (
            <aside className="poste-panneau" aria-label="Poste formateur">
                <button type="button" className="muted small lien" onClick={onFermer}>‹ Vue de la salle</button>

                <div className="poste-entete">
                    <h2>Poste formateur</h2>
                    {poste.etat && <span className="small muted">{LIBELLE_ETAT[poste.etat]}</span>}
                </div>
                <p className="muted small">Salle {salle.numSalle}</p>

                <div className="poste-actions">
                    {onAffecterFormateur && (
                        <button type="button" className="btn primary" onClick={onAffecterFormateur}>
                            Affecter un formateur
                        </button>
                    )}
                    <button type="button" className="btn">Plaquer un master</button>
                    <button type="button" className="btn">Affecter un matériel</button>
                </div>

                <h3>Affecté à</h3>
                {formateurs.length === 0 ? (
                    <p className="muted">Aucun formateur affecté à cette salle</p>
                ) : (
                    <ul>
                        {formateurs.map(f => (
                            <li key={f.id}>
                                {f.prenom} {f.nom}
                                {onRetirerFormateur && (
                                    <button type="button" className="muted small lien"
                                            onClick={() => onRetirerFormateur(f)}> Retirer</button>
                                )}
                            </li>
                        ))}
                    </ul>
                )}
            </aside>
        );
    }

    return (
        <aside className="poste-panneau" aria-label={`Poste ${numero}`}>
            <button type="button" className="muted small lien" onClick={onFermer}>‹ Vue de la salle</button>

            <div className="poste-entete">
                <h2>Poste {String(numero).padStart(2, "0")}</h2>
                {poste.etat && <span className="small muted">{LIBELLE_ETAT[poste.etat]}</span>}
            </div>
            <p className="muted small">Salle {salle.numSalle}</p>

            <div className="poste-actions">
                <button type="button" className="btn primary" onClick={onAffecterStagiaire}>
                    Affecter un stagiaire
                </button>
                <button type="button" className="btn">Plaquer un master</button>
                <button type="button" className="btn">Affecter un matériel</button>
            </div>

            <h3>Occupant</h3>
            <p className="muted">{poste.occupantId ? `Stagiaire n°${poste.occupantId}` : "Aucun stagiaire : poste disponible"}</p>
        </aside>
    );
}