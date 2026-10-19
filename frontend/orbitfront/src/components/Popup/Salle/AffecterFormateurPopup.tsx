import {useEffect, useState} from "react";
import Popup from "../Popup.tsx";
import type {Salle, Utilisateur} from "../../../types/types.ts";
import {affecterFormateurASalle, aujourdhuiIso, FormateurError, getFormateurs} from "../../../services/formateurService.ts";

interface Props {
    salle: Salle;
    onClose: () => void;
    onFait: () => void; // la salle a changé : la page se recharge
}

export default function AffecterFormateurPopup({salle, onClose, onFait}: Props) {
    const [formateurs, setFormateurs] = useState<Utilisateur[] | null>(null);
    const [choix, setChoix] = useState("");
    const [jusqu, setJusqu] = useState("");
    const [erreur, setErreur] = useState("");
    const [enCours, setEnCours] = useState(false);

    // formateurs actifs pas encore affectés à cette salle (un formateur peut avoir plusieurs salles)
    useEffect(() => {
        const dejaLa = new Set((salle.formateurs ?? []).map(f => f.id));
        getFormateurs()
            .then(liste => setFormateurs(liste.filter(f => f.active && !dejaLa.has(f.id))))
            .catch(() => setErreur("Impossible de charger les formateurs."));
    }, [salle]);

    async function valider() {
        if (!choix || !jusqu) return;
        setEnCours(true);
        setErreur("");
        try {
            await affecterFormateurASalle(Number(choix), salle.id, jusqu);
            onFait();
        } catch (err) {
            setErreur(err instanceof FormateurError && err.detail ? err.detail : "Affectation impossible.");
            setEnCours(false);
        }
    }

    return (
        <Popup
            open
            onClose={onClose}
            title={`Affecter un formateur à la salle ${salle.numSalle}`}
            description="Un formateur ne voit que les salles qui lui sont attribuées, jusqu'au soir de la date limite."
            confirmLabel="Affecter"
            onConfirm={valider}
            loading={enCours}
            disabled={!choix || !jusqu}
        >
            {formateurs !== null && formateurs.length === 0 ? (
                <p className="muted">Aucun formateur disponible : tous sont déjà affectés à cette salle.</p>
            ) : (
                <>
                    <label htmlFor="formateur">Formateur</label>
                    <select id="formateur" value={choix} onChange={(e) => setChoix(e.target.value)} autoFocus>
                        <option value="" disabled>Choisir un formateur…</option>
                        {(formateurs ?? []).map(f => (
                            <option key={f.id} value={f.id}>{f.prenom} {f.nom}</option>
                        ))}
                    </select>

                    <label htmlFor="jusqu">Accès jusqu'au</label>
                    <input id="jusqu" type="date" min={aujourdhuiIso()} value={jusqu}
                           onChange={(e) => setJusqu(e.target.value)} required/>
                </>
            )}
            {erreur && <p className="error-message" role="alert">{erreur}</p>}
        </Popup>
    );
}