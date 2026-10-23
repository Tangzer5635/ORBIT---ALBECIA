import {useState} from "react";
import Popup from "../Popup.tsx";
import {LIBELLE_LISTE_PROCESSUS, type ListeProcessus} from "../../../types/types.ts";
import {ajouterProcessus, ProcessusError} from "../../../services/processusService.ts";

interface Props {
    onClose: () => void;
    onFait: () => void;
}

// « Ajouter un processus » au référentiel : nom de l'exécutable + liste blanche ou noire
export default function AjouterProcessusPopup({onClose, onFait}: Props) {
    const [nom, setNom] = useState("");
    const [liste, setListe] = useState<ListeProcessus>("BLANCHE");
    const [erreur, setErreur] = useState("");
    const [enCours, setEnCours] = useState(false);

    async function valider() {
        if (!nom.trim()) {
            setErreur("Indiquez le nom du processus.");
            return;
        }
        setEnCours(true);
        setErreur("");
        try {
            await ajouterProcessus({nom: nom.trim(), liste});
            onFait();
        } catch (err) {
            setErreur(err instanceof ProcessusError && err.detail ? err.detail : "Impossible d'ajouter le processus.");
            setEnCours(false);
        }
    }

    return (
        <Popup
            open
            onClose={onClose}
            title="Ajouter un processus"
            description="Une fois référencé, un processus en liste blanche peut être ajouté à un master."
            confirmLabel="Ajouter le processus"
            onConfirm={valider}
            loading={enCours}
            disabled={!nom.trim()}
        >
            <label htmlFor="procNom">Nom de l'exécutable</label>
            <input id="procNom" className="mono" value={nom} onChange={e => setNom(e.target.value)}
                   maxLength={100} placeholder="Ex. eclipse.exe"/>

            <span className="popup-lbl" style={{marginTop: 12}}>Liste</span>
            <div className="quick" role="group" aria-label="Liste">
                {(Object.keys(LIBELLE_LISTE_PROCESSUS) as ListeProcessus[]).map(l => (
                    <button key={l} type="button" className="chip" aria-pressed={liste === l} onClick={() => setListe(l)}>
                        {LIBELLE_LISTE_PROCESSUS[l]}
                    </button>
                ))}
            </div>
            <p className="muted small">
                Liste blanche : autorisé, peut faire partie d'un master. Liste noire : interdit, il ne peut pas être lancé
                sur les postes ni ajouté à un master.
            </p>
            {erreur && <p className="error-message" role="alert">{erreur}</p>}
        </Popup>
    );
}