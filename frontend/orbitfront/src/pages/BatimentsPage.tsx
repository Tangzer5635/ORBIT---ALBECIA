import BatimentCard from "../components/BatimentCard.tsx";
import {type FormEvent, useState} from "react";
import {ajouterBatiment, BatimentError, supprimerBatiment} from "../services/batimentService.ts";
import {useBatiments} from "../context/BatimentsContext.tsx";
import {ANNEAU_VIDE} from "../utils/postes.ts";
import Legende from "../components/Legende.tsx";

export function BatimentsPage() {
    const {batiments, setBatiments, erreurChargement} = useBatiments(); // liste partagée avec la BatimentBar
    const [error, setError] = useState<string>();

    //useState pour le form
    const [formOuvert, setFormOuvert] = useState(false);
    const [numBatiment, setNumBatiment] = useState("");
    const [erreurForm, setErreurForm] = useState("");
    const [envoi, setEnvoi] = useState(false);

    function fermerForm() {
        setFormOuvert(false);
        setNumBatiment("");
        setErreurForm("");
    }

    async function handleSubmit(event: FormEvent<HTMLFormElement>) {
        event.preventDefault();

        // Champ obligatoire
        if (numBatiment.trim() === "") {
            setErreurForm("Le numéro du bâtiment est obligatoire.");
            return;
        }

        setEnvoi(true);
        try {
            const nouveau = await ajouterBatiment({ numBatiment: numBatiment.trim() });
            setBatiments((liste) => [...liste, { ...nouveau, anneau: ANNEAU_VIDE }]);

            // //ferme le form
            // setFormOuvert(false);
            // setNumBatiment("");
            // setErreurForm("");
            fermerForm()

        } catch (err) {
            if (err instanceof BatimentError && err.status === 403) {
                setErreurForm("Vous n'avez pas le droit de créer un bâtiment.");
            } else if (err instanceof BatimentError && err.status === 400) {
                setErreurForm("Numéro de bâtiment invalide.");
            } else {
                setErreurForm("Le service est indisponible. Réessayez plus tard.");
            }
        } finally {
            setEnvoi(false);
        }
    }

    async function handleSupprimer(id: number) {
        try {
            await supprimerBatiment(id);
            setBatiments((liste) => liste.filter((b) => b.id !== id));
        } catch (err) {
            if (err instanceof BatimentError && err.status === 403) setError("Vous n'avez pas le droit de supprimer un bâtiment.");
            else if (err instanceof BatimentError && err.status === 404) setError("Ce bâtiment n'existe plus.");
            else if (err instanceof BatimentError && err.status === 409) setError("Ce bâtiment contient encore des salles.");
            else setError("Le service est indisponible. Réessayez plus tard.");
        }
    }

    const nb = batiments.length;
    const nbSalles = batiments.reduce((acc, b) => acc + b.nbSalles, 0);
    const nbSallesStockage = batiments.reduce((acc, b) => acc + b.stockages.length, 0);


    return (
        <>
            <div className="page-head">
                <h1>Bâtiments</h1>
                <span className="muted">{nb} bâtiment{nb > 1 ? "s" : ""}, {nbSalles} salles, {nbSallesStockage} salles de stockage</span>
            </div>

            {!formOuvert && (
                <button type="button" className="btn primary" onClick={() => setFormOuvert(true)}>Créer un bâtiment</button>
            )}

            {formOuvert && (
                <form className="bat-form" onSubmit={handleSubmit} noValidate>
                    <label htmlFor="numBatiment">Numéro du bâtiment</label>
                    <input
                        id="numBatiment"
                        value={numBatiment}
                        onChange={(e) => setNumBatiment(e.target.value)}
                        autoFocus
                    />
                    <button type="submit" className="btn primary" disabled={envoi}>
                        {envoi ? "Création…" : "Créer"}
                    </button>
                    <button type="button" className="btn" onClick={fermerForm}>Annuler</button>

                    {erreurForm && <p className="error-message bat-form-erreur" role="alert">{erreurForm}</p>}
                </form>
            )}

            {(error ?? erreurChargement) && <p className="error-message" role="alert">{error ?? erreurChargement}</p>}
<br/><br/>
            <Legende/>

            <div className="bat-grid">
                {batiments.map((b) => (
                    <BatimentCard key={b.id} batiment={b} onSupprime={handleSupprimer}/>
                ))}
            </div>
        </>
    );
}