import {useEffect, useState} from "react";
import Popup from "../Popup.tsx";
import type {Master, Poste, Salle} from "../../../types/types.ts";
import {getMasters, MasterError, plaquerMaster, plaquerMasterSurPoste} from "../../../services/masterService.ts";
import {nomPoste, pluriel} from "../../../utils/postes.ts";

interface Props {
    salle: Salle;
    poste?: Poste; // présent : on plaque sur ce poste seulement (ex. poste en erreur)
    onClose: () => void;
    onFait: () => void;
}

// « Plaquer un master » depuis la page d'une salle : sur toute la salle, ou sur un seul poste
export default function PlaquerMasterSallePopup({salle, poste, onClose, onFait}: Props) {
    const [masters, setMasters] = useState<Master[] | null>(null);
    const [masterId, setMasterId] = useState(salle.masterId ? String(salle.masterId) : "");
    const [erreur, setErreur] = useState("");
    const [enCours, setEnCours] = useState(false);

    useEffect(() => {
        getMasters().then(setMasters).catch(() => setErreur("Impossible de charger les masters."));
    }, []);

    const choisi = masters?.find(m => m.id === Number(masterId));

    async function valider() {
        if (!masterId) return;
        setEnCours(true);
        setErreur("");
        try {
            if (poste) await plaquerMasterSurPoste(poste.id, Number(masterId));
            else await plaquerMaster(salle.id, Number(masterId));
            onFait();
        } catch (err) {
            setErreur(err instanceof MasterError && err.detail ? err.detail : "Impossible de plaquer le master.");
            setEnCours(false);
        }
    }

    return (
        <Popup
            open
            onClose={onClose}
            title={poste ? `Plaquer un master sur le ${nomPoste(poste).toLowerCase()}` : `Plaquer un master sur la salle ${salle.numSalle}`}
            description={poste
                ? "L'UC de ce poste reçoit exactement les processus du master : ses processus actuels sont remplacés. Le master de la salle ne change pas."
                : "Chaque UC des postes de la salle reçoit exactement les processus du master : ses processus actuels sont remplacés."}
            confirmLabel={salle.masterId && Number(masterId) === salle.masterId ? "Réinstaller" : "Plaquer"}
            onConfirm={valider}
            loading={enCours}
            disabled={!masterId}
        >
            {masters !== null && masters.length === 0 && (
                <p className="muted">Aucun master : créez-en un dans l'onglet Masters.</p>
            )}
            {masters !== null && masters.length > 0 && (
                <>
                    <label htmlFor="plaquerMaster">Master</label>
                    <select id="plaquerMaster" value={masterId} autoFocus onChange={e => setMasterId(e.target.value)}>
                        <option value="" disabled>Choisir un master…</option>
                        {masters.map(m => (
                            <option key={m.id} value={m.id}>
                                {m.nom} ({m.versionMaster}){m.id === salle.masterId ? " : master de la salle" : ""}
                            </option>
                        ))}
                    </select>
                </>
            )}
            {choisi && (
                <p className="muted small">
                    {pluriel(choisi.processus.length, "processus", "processus")} : {choisi.processus.map(p => p.nom).join(", ")}
                </p>
            )}
            {erreur && <p className="error-message" role="alert">{erreur}</p>}
        </Popup>
    );
}
