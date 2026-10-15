import {useEffect, useState} from "react";
import Popup from "./Popup.tsx";
import type {Utilisateur} from "../types/types.ts";
import {getStagiairesSansPoste} from "../services/stageService.ts";
import {affecterStagiaireAPoste, PosteError} from "../services/posteService.ts";

interface Props {
    posteId: number;
    numPoste: number;
    stageId: number | null | undefined; // stage accueilli par la salle
    onClose: () => void;
    onFait: () => void; // le poste a changé : la page se recharge
}

export default function AffecterStagiairePopup({posteId, numPoste, stageId, onClose, onFait}: Props) {
    const [stagiaires, setStagiaires] = useState<Utilisateur[] | null>(null);
    const [choix, setChoix] = useState("");
    const [erreur, setErreur] = useState("");
    const [enCours, setEnCours] = useState(false);

    // seuls les stagiaires du stage de la salle, pas encore assis, peuvent prendre le poste
    useEffect(() => {
        if (!stageId) return;
        getStagiairesSansPoste(stageId)
            .then(setStagiaires)
            .catch(() => setErreur("Impossible de charger les stagiaires."));
    }, [stageId]);

    async function valider() {
        if (!choix) return;
        setEnCours(true);
        setErreur("");
        try {
            await affecterStagiaireAPoste(posteId, Number(choix));
            onFait();
        } catch (err) {
            setErreur(err instanceof PosteError && err.detail ? err.detail : "Affectation impossible.");
            setEnCours(false);
        }
    }

    return (
        <Popup
            open
            onClose={onClose}
            title={`Affecter un stagiaire au poste ${numPoste}`}
            confirmLabel="Affecter"
            onConfirm={valider}
            loading={enCours}
            disabled={!choix}
        >
            {!stageId ? (
                <p className="muted">La salle n'accueille aucun stage : affecte d'abord un stage à la salle.</p>
            ) : stagiaires !== null && stagiaires.length === 0 ? (
                <p className="muted">Tous les stagiaires du stage ont déjà un poste.</p>
            ) : (
                <>
                    <label htmlFor="stagiaire">Stagiaire</label>
                    <select id="stagiaire" value={choix} onChange={(e) => setChoix(e.target.value)} autoFocus>
                        <option value="" disabled>Choisir un stagiaire…</option>
                        {(stagiaires ?? []).map(s => <option key={s.id} value={s.id}>{s.prenom} {s.nom}</option>)}
                    </select>
                </>
            )}
            {erreur && <p className="error-message" role="alert">{erreur}</p>}
        </Popup>
    );
}