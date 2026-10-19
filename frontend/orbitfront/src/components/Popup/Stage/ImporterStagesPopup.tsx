import {useState} from "react";
import Popup from "../Popup.tsx";
import {type BilanImportStages, LIBELLE_TYPE_FORMATION} from "../../../types/types.ts";
import {importerStages, StageError} from "../../../services/stageService.ts";
import {pluriel} from "../../../utils/postes.ts";

interface Props {
    onClose: () => void;
    onImporte: () => void; // des stages ont été créés ou modifiés : la page se recharge
}

const EN_TETE = "nom;type;dateDebut;dateFin;nidCDSF";
const EXEMPLE = `${EN_TETE}\nVU82;ESR;2026-08-07;2027-04-07;2000000001\nWB48;PDI;2026-08-29;2027-01-29;2000000010\n`;

// fichier d'exemple téléchargeable (même format que init/stages.csv)
const LIEN_EXEMPLE = URL.createObjectURL(new Blob([EXEMPLE], {type: "text/csv;charset=utf-8"}));

export default function ImporterStagesPopup({onClose, onImporte}: Props) {
    const [fichier, setFichier] = useState<File | null>(null);
    const [bilan, setBilan] = useState<BilanImportStages | null>(null);
    const [erreur, setErreur] = useState("");
    const [enCours, setEnCours] = useState(false);

    function choisir(f: File | undefined) {
        setErreur("");
        setBilan(null);
        if (f && !f.name.toLowerCase().endsWith(".csv")) {
            setFichier(null);
            setErreur("Choisissez un fichier .csv.");
            return;
        }
        setFichier(f ?? null);
    }

    async function importer() {
        if (!fichier) return;
        setEnCours(true);
        setErreur("");
        try {
            const resultat = await importerStages(fichier);
            setBilan(resultat);
            if (resultat.crees.length || resultat.misAJour.length) onImporte();
        } catch (err) {
            setErreur(err instanceof StageError && err.detail ? err.detail : "Import impossible.");
        } finally {
            setEnCours(false);
        }
    }

    return (
        <Popup
            open
            onClose={onClose}
            title="Importer des stages"
            confirmLabel={bilan ? "Fermer" : "Importer"}
            onConfirm={bilan ? onClose : importer}
            loading={enCours}
            disabled={!bilan && !fichier}
        >
            <p className="muted small">
                Fichier CSV, séparateur « ; », avec l'en-tête <code>{EN_TETE}</code>. Un stage dont le nom existe
                déjà est mis à jour, les autres sont créés. Une ligne en erreur est ignorée, les autres sont importées.
            </p>
            <ul className="muted small import-aide">
                <li><b>nom</b> : 2 lettres puis 2 chiffres (ex. WB47)</li>
                <li><b>type</b> : {Object.entries(LIBELLE_TYPE_FORMATION).map(([code, l]) => `${code} (${l})`).join(", ")}</li>
                <li><b>dateDebut</b>, <b>dateFin</b> : 2026-08-07 ou 07/08/2026</li>
                <li><b>nidCDSF</b> : NID à 10 chiffres du CDSF (Chef de Section Formation) qui gère le stage, un formateur existant</li>
            </ul>
            <a className="btn sm" href={LIEN_EXEMPLE} download="exemple-stages.csv">Télécharger un exemple</a>

            <label htmlFor="fichier">Fichier</label>
            <input id="fichier" type="file" accept=".csv,text/csv" onChange={e => choisir(e.target.files?.[0])}/>

            {bilan && (
                <div className="import-bilan">
                    <p>
                        <b>{pluriel(bilan.crees.length, "stage créé", "stages créés")}</b>
                        {bilan.crees.length > 0 && ` : ${bilan.crees.join(", ")}`}
                    </p>
                    <p>
                        <b>{pluriel(bilan.misAJour.length, "stage mis à jour", "stages mis à jour")}</b>
                        {bilan.misAJour.length > 0 && ` : ${bilan.misAJour.join(", ")}`}
                    </p>
                    {bilan.erreurs.length > 0 && (
                        <>
                            <p className="error-message">{pluriel(bilan.erreurs.length, "ligne rejetée", "lignes rejetées")}</p>
                            <ul className="import-erreurs">
                                {bilan.erreurs.map(e => <li key={e}>{e}</li>)}
                            </ul>
                        </>
                    )}
                </div>
            )}
            {erreur && <p className="error-message" role="alert">{erreur}</p>}
        </Popup>
    );
}