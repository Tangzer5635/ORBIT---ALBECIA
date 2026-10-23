import {useState} from "react";
import Popup from "../Popup.tsx";
import type {Batiment, TypeSalle} from "../../../types/types.ts";
import {creerSalleEquipee, creerSalleStockage, SalleError} from "../../../services/salleService.ts";
import {ETAGES, pluriel} from "../../../utils/postes.ts";

const ETAPES = ["Identité", "Composition", "Récapitulatif"];

interface Props {
    batiment: Batiment;
    onClose: () => void;
    onCree: () => void;
}

export default function CreerSallePopup({batiment, onClose, onCree}: Props) {
    const [etape, setEtape] = useState(1);
    const [type, setType] = useState<TypeSalle>("SalleDeCours");
    const [numSalle, setNumSalle] = useState("");
    const [etage, setEtage] = useState("0");
    const [nbPostes, setNbPostes] = useState(12);
    const [ecransParPoste, setEcransParPoste] = useState<1 | 2>(1);
    const [nbVideoProjecteurs, setNbVideoProjecteurs] = useState(1);
    const [nbTableaux, setNbTableaux] = useState(0);
    const [erreur, setErreur] = useState("");
    const [envoi, setEnvoi] = useState(false);

    const stockage = type === "SalleStockage";
    const derniere = stockage ? 1 : 3; // une salle de stockage se crée vide : pas de composition

    async function suivant() {
        setErreur("");
        if (etape === 1) {
            if (numSalle.trim() === "") return setErreur("Le numéro de la salle est obligatoire.");
            if (!stockage) return setEtape(2);
        }
        if (etape === 2) {
            if (!Number.isInteger(nbPostes) || nbPostes < 0) return setErreur("Nombre de postes invalide.");
            return setEtape(3);
        }

        setEnvoi(true);
        try {
            if (stockage) {
                await creerSalleStockage(batiment.id, numSalle.trim(), etage);
            } else {
                const {manquants} = await creerSalleEquipee({
                    idBatiment: batiment.id,
                    numSalle: numSalle.trim(),
                    etage,
                    nbPostes,
                    ecransParPoste,
                    nbVideoProjecteurs,
                    nbTableaux,
                });
                const manque = Object.entries(manquants).map(([t, nb]) => `${nb} ${t}`);
                if (manque.length > 0) console.log(`Salle ${numSalle} créée incomplète, plus assez en stock : il manque ${manque.join(", ")}`);
            }
            onCree();
        } catch (err) {
            if (err instanceof SalleError && err.status === 403) setErreur("Vous n'avez pas le droit de créer une salle.");
            else if (err instanceof SalleError && err.status === 400) setErreur(err.detail || "Données invalides : ce numéro de salle existe peut-être déjà.");
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
            title={stockage ? "Créer une salle de stockage" : "Créer une salle"}
            confirmLabel={etape < derniere ? "Suivant" : "Créer la salle"}
            onConfirm={suivant}
            cancelLabel={etape > 1 ? "Précédent" : undefined}
            onCancel={etape > 1 ? () => { setErreur(""); setEtape(etape - 1); } : undefined}
            loading={envoi}
        >
            {!stockage && (
                <ol className="popup-etapes">
                    {ETAPES.map((nom, i) => (
                        <li key={nom} className={i < etape ? "faite" : ""}>Étape {i + 1} : {nom}</li>
                    ))}
                </ol>
            )}

            {etape === 1 && (
                <>
                    <span id="type-salle-label" className="label">Type de salle</span>
                    <div className="seg" role="group" aria-labelledby="type-salle-label">
                        <button type="button" aria-pressed={!stockage} onClick={() => setType("SalleDeCours")}>
                            Salle de cours
                        </button>
                        <button type="button" aria-pressed={stockage} onClick={() => setType("SalleStockage")}>
                            Salle de stockage
                        </button>
                    </div>

                    <label htmlFor="numSalle">Numéro de la salle</label>
                    <input
                        id="numSalle"
                        placeholder="Ex. 204"
                        maxLength={10}
                        value={numSalle}
                        onChange={(e) => { setNumSalle(e.target.value); setErreur(""); }}
                    />
                    <label htmlFor="etage">Étage</label>
                    <select id="etage" value={etage} onChange={(e) => setEtage(e.target.value)}>
                        {ETAGES.map((nom, i) => <option key={nom} value={String(i)}>{nom}</option>)}
                    </select>
                    {stockage && (
                        <p className="muted small">La salle de stockage est créée vide, le matériel y est ajouté ensuite.</p>
                    )}
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



                    <span id="ecrans-par-poste-label" className="label">Écrans par poste</span>
                    <div className="seg" role="group" aria-labelledby="ecrans-par-poste-label">
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
                        <li>{pluriel(nbPostes, "poste")} stagiaire{nbPostes > 1 ? "s" : ""} + 1 poste formateur</li>
                        <li>{pluriel(ecransParPoste, "écran")} par poste</li>
                        <li>{pluriel(nbVideoProjecteurs, "vidéo-projecteur")}</li>
                        <li>{pluriel(nbTableaux, "tableau interactif", "tableaux interactifs")}</li>
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