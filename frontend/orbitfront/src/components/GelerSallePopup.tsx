import {useState} from "react";
import Popup from "./Popup.tsx";
import {LIBELLE_MOTIF_GEL, type MotifGel, type Salle} from "../types/types.ts";
import {gelerSalle, SalleError} from "../services/salleService.ts";

interface Props {
    salle: Salle;
    onClose: () => void;
    onFait: () => void;
}

export default function GelerSallePopup({salle, onClose, onFait}: Props) {
    const [motif, setMotif] = useState<MotifGel | null>(null);
    const [note, setNote] = useState("");
    const [jusqu, setJusqu] = useState("");
    const [erreur, setErreur] = useState("");
    const [enCours, setEnCours] = useState(false);

    async function valider() {
        if (!motif) {
            setErreur("Choisissez un motif.");
            return;
        }
        setEnCours(true);
        setErreur("");
        try {
            await gelerSalle(salle.id, {motif, note: note.trim() || undefined, jusqu: jusqu || undefined});
            onFait();
        } catch (err) {
            setErreur(err instanceof SalleError && err.detail ? err.detail : "Impossible de geler la salle.");
            setEnCours(false);
        }
    }

    return (
        <Popup
            open
            onClose={onClose}
            title={`Geler la salle ${salle.numSalle}`}
            description="Une salle gelée reste visible mais ne compte plus dans la disponibilité du bâtiment. On ne peut plus y affecter de stage, de stagiaire ni de master tant qu'elle n'est pas dégelée."
            confirmLabel="Geler la salle"
            onConfirm={valider}
            loading={enCours}
            disabled={!motif}
        >
            <span className="popup-lbl">Motif</span>
            <div className="quick" role="group" aria-label="Motif">
                {(Object.keys(LIBELLE_MOTIF_GEL) as MotifGel[]).map(m => (
                    <button key={m} type="button" className="chip" aria-pressed={motif === m}
                            onClick={() => setMotif(m)}>
                        {LIBELLE_MOTIF_GEL[m]}
                    </button>
                ))}
            </div>

            <label htmlFor="gelNote">Précision (facultatif)</label>
            <input id="gelNote" value={note} onChange={e => setNote(e.target.value)} maxLength={200}
                   placeholder="Ex. remplacement du faux plafond"/>

            <label htmlFor="gelJusqu">Fin prévue (facultatif)</label>
            <input id="gelJusqu" type="date" value={jusqu} min={new Date().toLocaleDateString("sv-SE")}
                   onChange={e => setJusqu(e.target.value)}/>

            {salle.stageNom && (
                <p className="note-chg">Le stage {salle.stageNom} est affecté à cette salle. Il y reste : pensez à le déplacer si besoin.</p>
            )}
            {erreur && <p className="error-message" role="alert">{erreur}</p>}
        </Popup>
    );
}