import {useEffect, useState} from "react";
import Popup from "../Popup.tsx";
import type {Salle, Master} from "../../../types/types.ts";
import {getMasters, MasterError, plaquerMaster} from "../../../services/masterService.ts";


interface Props {
    salle: Salle;
    onClose: () => void;
    onFait: () => void; // la salle a changé : la page se recharge
}

export default function AffecterSallePopup({salle, onClose, onFait}: Props) {
    const [masters, setMasters] = useState<Master[] | null>(null);
    const [choix, setChoix] = useState("");
    const [erreur, setErreur] = useState("");
    const [enCours, setEnCours] = useState(false);

    useEffect(() => {
        getMasters()
            .then(liste => setMasters(liste))
            .catch(() => setErreur("Impossible de charger les masters."));
    }, [salle]);

    async function valider() {
        if (!choix) return;
        setEnCours(true);
        setErreur("");
        try {
            await plaquerMaster(Number(choix), salle.id);
            onFait();
        } catch (err) {
            setErreur(err instanceof MasterError && err.detail ? err.detail : "Affectation impossible.");
            setEnCours(false);
        }
    }

    return (
        <Popup
            open
            onClose={onClose}
            title={`Affecter un formateur à la salle ${salle.numSalle}`}
            confirmLabel="Affecter"
            onConfirm={valider}
            loading={enCours}
            disabled={!choix}
        >
            {masters !== null && masters.length === 0 ? (
                <p className="muted">Aucun formateur disponible : tous sont déjà affectés à cette salle.</p>
            ) : (
                <>
                    <label htmlFor="formateur">Formateur</label>
                    <select id="formateur" value={choix} onChange={(e) => setChoix(e.target.value)}>
                        <option value="" disabled>Choisir un formateur…</option>
                        {(masters ?? []).map(f => (
                            <option key={f.id} value={f.id}>{f.nom}</option>
                        ))}
                    </select>
                </>
            )}
            {erreur && <p className="error-message" role="alert">{erreur}</p>}
        </Popup>
    );
}
