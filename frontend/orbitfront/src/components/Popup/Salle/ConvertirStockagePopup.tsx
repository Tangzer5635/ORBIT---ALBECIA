import {useState} from "react";
import Popup from "../Popup.tsx";
import type {SalleCarte} from "../../../types/types.ts";
import {convertirEnSalleDeCours, SalleError} from "../../../services/salleService.ts";

interface Props {
    salle: SalleCarte;
    autresStockages: SalleCarte[]; // les autres salles de stockage du bâtiment
    onClose: () => void;
    onFait: () => void;
}

// Salle de stockage -> salle de cours : VP et TBI restent, le reste part dans un autre stockage du bâtiment
export default function ConvertirStockagePopup({salle, autresStockages, onClose, onFait}: Props) {
    const [cible, setCible] = useState<number | undefined>(autresStockages[0]?.id);
    const [erreur, setErreur] = useState("");
    const [enCours, setEnCours] = useState(false);
    const seule = autresStockages.length === 0;

    async function valider() {
        setEnCours(true);
        setErreur("");
        try {
            await convertirEnSalleDeCours(salle.id, cible);
            onFait();
        } catch (err) {
            setErreur(err instanceof SalleError && err.detail ? err.detail : "Conversion impossible.");
            setEnCours(false);
        }
    }

    return (
        <Popup
            open
            onClose={onClose}
            title={`Convertir la salle ${salle.numSalle} en salle de cours`}
            description="Les vidéoprojecteurs et tableaux interactifs restent dans la salle, le reste du matériel part dans la salle de stockage choisie. La salle est créée sans poste : équipez-la ensuite avec « Modifier la salle »."
            confirmLabel="Convertir"
            onConfirm={valider}
            loading={enCours}
            disabled={seule}
        >
            {seule ? (
                <p className="error-message">
                    C'est la seule salle de stockage du bâtiment : créez-en une autre avant de la convertir.
                </p>
            ) : (
                <>
                    <label htmlFor="stockageCible">Déplacer le matériel vers</label>
                    <select id="stockageCible" value={cible ?? ""} onChange={(e) => setCible(Number(e.target.value))}>
                        {autresStockages.map(s => <option key={s.id} value={s.id}>Salle {s.numSalle}</option>)}
                    </select>
                </>
            )}
            {erreur && <p className="error-message" role="alert">{erreur}</p>}
        </Popup>
    );
}