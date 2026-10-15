import {useEffect, useState} from "react";
import Popup from "./Popup.tsx";
import {LIBELLE_TYPE_MATERIEL, type Materiel} from "../types/types.ts";
import {affecterMaterielAPoste, getMateriels, MaterielError} from "../services/materielService.ts";
import {ORDRE_TYPES} from "../utils/materiels.ts";

interface Props {
    posteId: number;
    titrePoste: string; // "Poste 04", "Poste formateur"
    onClose: () => void;
    onFait: () => void; // le poste a changé : la page se recharge
}

export default function AffecterMaterielPopup({posteId, titrePoste, onClose, onFait}: Props) {
    const [stock, setStock] = useState<Materiel[] | null>(null);
    const [choix, setChoix] = useState("");
    const [erreur, setErreur] = useState("");
    const [enCours, setEnCours] = useState(false);

    // seul le matériel en stock peut être affecté
    useEffect(() => {
        getMateriels({localisation: "STOCK"}, 0, 500)
            .then(page => setStock(page.content))
            .catch(() => setErreur("Impossible de charger le stock."));
    }, []);

    async function valider() {
        if (!choix) return;
        setEnCours(true);
        setErreur("");
        try {
            await affecterMaterielAPoste(Number(choix), posteId);
            onFait();
        } catch (err) {
            setErreur(err instanceof MaterielError && err.detail ? err.detail : "Affectation impossible.");
            setEnCours(false);
        }
    }

    return (
        <Popup
            open
            onClose={onClose}
            title={`Affecter un matériel : ${titrePoste}`}
            confirmLabel="Affecter"
            onConfirm={valider}
            loading={enCours}
            disabled={!choix}
        >
            {stock !== null && stock.length === 0 ? (
                <p className="muted">Aucun matériel en stock.</p>
            ) : (
                <>
                    <label htmlFor="materiel">Matériel en stock</label>
                    <select id="materiel" value={choix} onChange={(e) => setChoix(e.target.value)} autoFocus>
                        <option value="" disabled>Choisir un matériel…</option>
                        {ORDRE_TYPES.map(type => {
                            const duType = (stock ?? []).filter(m => m.type === type);
                            return duType.length > 0 && (
                                <optgroup key={type} label={LIBELLE_TYPE_MATERIEL[type]}>
                                    {duType.map(m => (
                                        <option key={m.id} value={m.id}>
                                            {m.modele} ({m.numSerie}){m.numSalle ? `, réserve ${m.numSalle}` : ""}
                                        </option>
                                    ))}
                                </optgroup>
                            );
                        })}
                    </select>
                </>
            )}
            {erreur && <p className="error-message" role="alert">{erreur}</p>}
        </Popup>
    );
}