import {useState} from "react";
import Popup from "../Popup.tsx";

type Props = {
    du: string;      // "2026-10-01"
    au: string;
    nombre: number;  // nombre d'entrées exportées
    csv: string;
    onClose: () => void;
};

const frDate = (iso: string) => iso.split("-").reverse().join("/");

// Aperçu du CSV du journal, avec téléchargement et copie (comme la maquette)
export default function ExporterJournalPopup({du, au, nombre, csv, onClose}: Props) {
    const [message, setMessage] = useState<{ ok: boolean; texte: string }>();
    const fichier = `journal-orbit_${du}_${au}.csv`;

    function telecharger() {
        // le BOM permet à Excel de lire correctement les accents
        const url = URL.createObjectURL(new Blob(["\uFEFF" + csv], {type: "text/csv;charset=utf-8"}));
        const lien = document.createElement("a");
        lien.href = url;
        lien.download = fichier;
        document.body.appendChild(lien);
        lien.click();
        lien.remove();
        setTimeout(() => URL.revokeObjectURL(url), 1000);
    }

    async function copier() {
        try {
            await navigator.clipboard.writeText(csv);
            setMessage({ok: true, texte: "Copié dans le presse-papiers."});
        } catch {
            setMessage({ok: false, texte: "Sélectionnez le texte et copiez-le manuellement."});
        }
    }

    return (
        <Popup
            open
            onClose={onClose}
            title="Exporter le journal"
            description={<>{nombre} entrée{nombre > 1 ? "s" : ""} du {frDate(du)} au {frDate(au)}, au format CSV
                (séparateur « ; »). Fichier : <b>{fichier}</b></>}
            confirmLabel="Télécharger"
            onConfirm={telecharger}
            cancelLabel="Fermer"
        >
            <textarea className="mono" rows={11} readOnly value={csv} aria-label="Contenu du fichier CSV"/>
            <button type="button" className="btn sm journal-copier" onClick={() => void copier()}>Copier</button>
            {message && <p className={message.ok ? "muted" : "error-message"} role="status">{message.texte}</p>}
        </Popup>
    );
}