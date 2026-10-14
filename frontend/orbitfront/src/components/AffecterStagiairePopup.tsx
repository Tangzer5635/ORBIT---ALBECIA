import {useEffect, useState} from "react";
import Popup from "./Popup.tsx";
import type {Utilisateur} from "../types/types.ts";
import {getUtilisateurs} from "../services/utilisateurService.ts";

interface Props {
    numPoste: number;
    idsDejaPlaces: number[]; // stagiaires qui ont déjà un poste dans cette salle
    onClose: () => void;
    onAffecte: (stagiaireId: number) => void;
}

export default function AffecterStagiairePopup({numPoste, idsDejaPlaces, onClose, onAffecte}: Props) {
    const [stagiaires, setStagiaires] = useState<Utilisateur[]>([]);
    const [choix, setChoix] = useState("");
    const [erreur, setErreur] = useState("");

    useEffect(() => {
        getUtilisateurs()
            .then(us => setStagiaires(us.filter(u => u.role === "STAGIAIRE" && u.active && !idsDejaPlaces.includes(u.id))))
            .catch(() => setErreur("Impossible de charger les stagiaires."));
    }, [idsDejaPlaces]);

    return (
        <Popup
            open
            onClose={onClose}
            title={`Affecter un stagiaire au poste ${numPoste}`}
            confirmLabel="Affecter"
            onConfirm={() => choix && onAffecte(Number(choix))}
            disabled={!choix}
        >
            <label htmlFor="stagiaire">Stagiaire</label>
            <select id="stagiaire" value={choix} onChange={(e) => setChoix(e.target.value)} autoFocus>
                <option value="" disabled>Choisir un stagiaire…</option>
                {stagiaires.map(s => <option key={s.id} value={s.id}>{s.prenom} {s.nom}</option>)}
            </select>
            {erreur && <p className="error-message" role="alert">{erreur}</p>}
        </Popup>
    );
}