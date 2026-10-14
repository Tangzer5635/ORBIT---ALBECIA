import {useEffect, useState} from "react";
import Popup from "./Popup.tsx";
import {LIBELLE_TYPE_FORMATION, type Salle, type Stage} from "../types/types.ts";
import {affecterStageASalle, getStages, StageError} from "../services/stageService.ts";
import {statutDe} from "../utils/stages.ts";

interface Props {
    salle: Salle;
    onClose: () => void;
    onFait: () => void; // la salle a changé : la page se recharge
}

export default function AffecterStagePopup({salle, onClose, onFait}: Props) {
    const [stages, setStages] = useState<Stage[] | null>(null);
    const [choix, setChoix] = useState("");
    const [erreur, setErreur] = useState("");
    const [enCours, setEnCours] = useState(false);

    // seulement les stages pas encore commencés ou en cours, et sans salle
    useEffect(() => {
        getStages()
            .then(liste => setStages(liste.filter(s => statutDe(s) !== "TERMINE" && !s.salleId)))
            .catch(() => setErreur("Impossible de charger les stages."));
    }, []);

    async function valider() {
        if (!choix) return;
        setEnCours(true);
        setErreur("");
        try {
            await affecterStageASalle(Number(choix), salle.id);
            onFait();
        } catch (err) {
            setErreur(err instanceof StageError && err.detail ? err.detail : "Affectation impossible.");
            setEnCours(false);
        }
    }

    return (
        <Popup
            open
            onClose={onClose}
            title={`Affecter un stage à la salle ${salle.numSalle}`}
            confirmLabel="Affecter"
            onConfirm={valider}
            loading={enCours}
            disabled={!choix}
        >
            {stages !== null && stages.length === 0 ? (
                <p className="muted">Aucun stage disponible : tous ont déjà une salle ou sont terminés.</p>
            ) : (
                <>
                    <label htmlFor="stage">Stage</label>
                    <select id="stage" value={choix} onChange={(e) => setChoix(e.target.value)} autoFocus>
                        <option value="" disabled>Choisir un stage…</option>
                        {(stages ?? []).map(s => (
                            <option key={s.id} value={s.id}>{s.nom} ({LIBELLE_TYPE_FORMATION[s.type]})</option>
                        ))}
                    </select>
                </>
            )}
            {erreur && <p className="error-message" role="alert">{erreur}</p>}
        </Popup>
    );
}
