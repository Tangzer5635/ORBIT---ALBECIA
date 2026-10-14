import {useState} from "react";
import Popup from "./Popup.tsx";
import type {Batiment} from "../types/types.ts";
import {creerSalleEquipee, SalleError} from "../services/salleService.ts";
import {pluriel} from "../utils/postes.ts";

const ETAGES = ["Rez-de-chaussée", "1er étage", "2e étage", "3e étage"]; // l'index est la valeur envoyée
const ETAPES = ["Identité", "Composition", "Récapitulatif"];

interface Props {
    batiment: Batiment;
    onClose: () => void;
    onCree: () => void;
}

export default function CreerSallePopup({batiment, onClose, onCree}: Props) {
    const [etape, setEtape] = useState(1);
    const [numSalle, setNumSalle] = useState("");
    const [etage, setEtage] = useState("0");
    const [nbPostes, setNbPostes] = useState(12);
    const [ecransParPoste, setEcransParPoste] = useState<1 | 2>(1);
    const [nbVideoProjecteurs, setNbVideoProjecteurs] = useState(1);
    const [nbTableaux, setNbTableaux] = useState(0);
    const [erreur, setErreur] = useState("");
    const [envoi, setEnvoi] = useState(false);

    async function suivant() {
        setErreur("");
        if (etape === 1) {
            if (numSalle.trim() === "") return setErreur("Le numéro de la salle est obligatoire.");
            return setEtape(2);
        }
        if (etape === 2) {
            if (!Number.isInteger(nbPostes) || nbPostes < 0) return setErreur("Nombre de postes invalide.");
            return setEtape(3);
        }

        setEnvoi(true);
        try {
            const {manquants} = await creerSalleEquipee({
                idBatiment: batiment.id,
                numSalle: numSalle.trim(),
                etage,
                nbPostes,
                ecransParPoste,
                nbVideoProjecteurs,
                nbTableaux,
            });
            const manque = Object.entries(manquants).map(([type, nb]) => `${nb} ${type}`);
            if (manque.length > 0) console.log(`Salle ${numSalle} créée incomplète, plus assez en stock : il manque ${manque.join(", ")}`);
            onCree();
        } catch (err) {
            if (err instanceof SalleError && err.status === 403) setErreur("Vous n'avez pas le droit de créer une salle.");
            else if (err instanceof SalleError && err.status === 400) setErreur("Données invalides : ce numéro de salle existe peut-être déjà.");
            else setErreur("Le service est indisponible. Réessayez plus tard.");
        } finally {
            setEnvoi(false);
        }
    }

    const nbPostesTotal = nbPostes + 1; // + le poste formateur

    return (
        <Popup
            open
            onClose={onClose}
            title="Créer une salle"
            confirmLabel={etape < 3 ? "Suivant" : "Créer la salle"}
            onConfirm={suivant}
            cancelLabel={etape > 1 ? "Précédent" : undefined}
            onCancel={etape > 1 ? () => { setErreur(""); setEtape(etape - 1); } : undefined}
            loading={envoi}
        >
            <ol className="popup-etapes">
                {ETAPES.map((nom, i) => (
                    <li key={nom} className={i < etape ? "faite" : ""}>Étape {i + 1} : {nom}</li>
                ))}
            </ol>

            {etape === 1 && (
                <>
                    <label htmlFor="numSalle">Numéro de la salle</label>
                    <input
                        id="numSalle"
                        placeholder="Ex. 204"
                        value={numSalle}
                        onChange={(e) => { setNumSalle(e.target.value); setErreur(""); }}
                        autoFocus
                    />
                    <label htmlFor="etage">Étage</label>
                    <select id="etage" value={etage} onChange={(e) => setEtage(e.target.value)}>
                        {ETAGES.map((nom, i) => <option key={nom} value={String(i)}>{nom}</option>)}
                    </select>
                </>
            )}

            {etape === 2 && (
                <>
                    <label htmlFor="nbPostes">Postes stagiaires</label>
                    <input
                        id="nbPostes"
                        type="number"
                        min={0}
                        value={nbPostes}
                        onChange={(e) => setNbPostes(Number(e.target.value))}
                    />
                    <p className="muted small">Un poste formateur est toujours ajouté et équipé en priorité.</p>

                    <label>Écrans par poste</label>
                    <div className="seg">
                        {([1, 2] as const).map((n) => (
                            <button key={n} type="button" aria-pressed={ecransParPoste === n} onClick={() => setEcransParPoste(n)}>
                                {n}
                            </button>
                        ))}
                    </div>

                    <label>Matériel de salle</label>
                    <Compteur label="Vidéoprojecteur" valeur={nbVideoProjecteurs} onChange={setNbVideoProjecteurs}/>
                    <Compteur label="Tableau interactif" valeur={nbTableaux} onChange={setNbTableaux}/>
                </>
            )}

            {etape === 3 && (
                <>
                    <p>Bâtiment {batiment.numBatiment}, salle {numSalle.trim()}, {ETAGES[Number(etage)].toLowerCase()} :</p>
                    <ul className="popup-recap">
                        <li>{nbPostes} {pluriel(nbPostes, "poste")} stagiaire + 1 poste formateur</li>
                        <li>{ecransParPoste} {pluriel(ecransParPoste, "écran")} par poste</li>
                        <li>{nbVideoProjecteurs} {pluriel(nbVideoProjecteurs, "vidéo-projecteur")}</li>
                        <li>{nbTableaux} tableau{nbTableaux > 1 ? "x" : ""} interactif{nbTableaux > 1 ? "s" : ""}</li>
                    </ul>
                    <p className="muted small">
                        Soit {nbPostesTotal} unités centrales et {nbPostesTotal * ecransParPoste} écrans pris dans le stock du bâtiment.
                        S'il en manque, la salle est créée avec ce qui est disponible.
                    </p>
                </>
            )}

            {erreur && <p className="error-message" role="alert">{erreur}</p>}
        </Popup>
    );
}

function Compteur({label, valeur, onChange}: { label: string; valeur: number; onChange: (n: number) => void }) {
    return (
        <div className="compteur">
            <span>{label}</span>
            <button type="button" onClick={() => onChange(valeur - 1)} disabled={valeur === 0} aria-label={`Retirer : ${label}`}>−</button>
            <b>{valeur}</b>
            <button type="button" onClick={() => onChange(valeur + 1)} aria-label={`Ajouter : ${label}`}>+</button>
        </div>
    );
}