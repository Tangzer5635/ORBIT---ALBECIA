import {useState} from "react";
import Popup from "../Popup.tsx";
import type {SalleCarte} from "../../../types/types.ts";
import {modifierStockage, SalleError} from "../../../services/salleService.ts";
import {ETAGES, libelleEtage} from "../../../utils/postes.ts";

interface Props {
    salle: SalleCarte;
    onClose: () => void;
    onFait: () => void;
}

// Numéro et étage d'une salle de stockage
export default function ModifierStockagePopup({salle, onClose, onFait}: Props) {
    const [numSalle, setNumSalle] = useState(salle.numSalle);
    const [etage, setEtage] = useState(salle.etage);
    const [erreur, setErreur] = useState("");
    const [enCours, setEnCours] = useState(false);

    // étage hors de la liste (ex. "5") : on le garde dans les choix
    const etages = ETAGES.map((nom, i) => ({valeur: String(i), nom}));
    if (!etages.some(e => e.valeur === salle.etage)) etages.push({valeur: salle.etage, nom: libelleEtage(salle.etage)});

    async function valider() {
        if (numSalle.trim() === "") return setErreur("Le numéro de la salle est obligatoire.");
        setEnCours(true);
        setErreur("");
        try {
            await modifierStockage(salle.id, numSalle.trim(), etage);
            onFait();
        } catch (err) {
            setErreur(err instanceof SalleError && err.detail ? err.detail : "Modification impossible.");
            setEnCours(false);
        }
    }

    return (
        <Popup
            open
            onClose={onClose}
            title={`Modifier la salle de stockage ${salle.numSalle}`}
            confirmLabel="Enregistrer"
            onConfirm={valider}
            loading={enCours}
        >
            <label htmlFor="numStockage">Numéro de la salle</label>
            <input id="numStockage" maxLength={10} value={numSalle}
                   onChange={(e) => { setNumSalle(e.target.value); setErreur(""); }} autoFocus/>

            <label htmlFor="etageStockage">Étage</label>
            <select id="etageStockage" value={etage} onChange={(e) => setEtage(e.target.value)}>
                {etages.map(e => <option key={e.valeur} value={e.valeur}>{e.nom}</option>)}
            </select>

            {erreur && <p className="error-message" role="alert">{erreur}</p>}
        </Popup>
    );
}