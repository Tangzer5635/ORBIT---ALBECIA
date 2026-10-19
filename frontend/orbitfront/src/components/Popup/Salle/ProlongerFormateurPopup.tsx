import {useState} from "react";
import Popup from "../Popup.tsx";
import type {Formateur, Salle} from "../../../types/types.ts";
import {aujourdhuiIso, FormateurError, prolongerFormateur} from "../../../services/formateurService.ts";
import {dateFr} from "../../../utils/postes.ts";

interface Props {
    salle: Salle;
    formateur: Formateur;
    onClose: () => void;
    onFait: () => void; // la salle a changé : la page se recharge
}

// Nouvelle date limite d'accès d'un formateur déjà attribué (aussi pour réactiver un accès expiré)
export default function ProlongerFormateurPopup({salle, formateur, onClose, onFait}: Props) {
    const [jusqu, setJusqu] = useState(formateur.expire ? "" : formateur.jusqu ?? "");
    const [erreur, setErreur] = useState("");
    const [enCours, setEnCours] = useState(false);

    async function valider() {
        if (!jusqu) return;
        setEnCours(true);
        setErreur("");
        try {
            await prolongerFormateur(formateur.id, salle.id, jusqu);
            onFait();
        } catch (err) {
            setErreur(err instanceof FormateurError && err.detail ? err.detail : "Modification impossible.");
            setEnCours(false);
        }
    }

    const actuelle = formateur.jusqu
        ? `Accès actuel jusqu'au ${dateFr(formateur.jusqu)}${formateur.expire ? " (expiré)" : ""}.`
        : "Accès actuel sans date limite.";

    return (
        <Popup
            open
            onClose={onClose}
            title={`Prolonger ${formateur.prenom} ${formateur.nom}`}
            description={`${actuelle} Le formateur garde l'accès à la salle ${salle.numSalle} jusqu'au soir de la nouvelle date.`}
            confirmLabel="Enregistrer"
            onConfirm={valider}
            loading={enCours}
            disabled={!jusqu}
        >
            <label htmlFor="jusqu">Accès jusqu'au</label>
            <input id="jusqu" type="date" min={aujourdhuiIso()} value={jusqu}
                   onChange={(e) => setJusqu(e.target.value)} autoFocus required/>
            {erreur && <p className="error-message" role="alert">{erreur}</p>}
        </Popup>
    );
}