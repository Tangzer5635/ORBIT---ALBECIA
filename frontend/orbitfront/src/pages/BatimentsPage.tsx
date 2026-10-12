import BatimentCard from "../components/BatimentComponents.tsx";
import type {Batiment} from "../types/types.ts";
import {type FormEvent, useEffect, useState} from "react";
import {ajouterBatiment, BatimentError, getBatiments, supprimerBatiment} from "../services/batimentService.ts";

export function BatimentsPage() {
    const [batiments, setBatiments] = useState<Batiment[]>([]);
    const [error, setError] = useState<string>();

    //useState pour le form
    const [formOuvert, setFormOuvert] = useState(false);
    const [numBatiment, setNumBatiment] = useState("");
    const [erreurForm, setErreurForm] = useState("");
    const [envoi, setEnvoi] = useState(false);

    useEffect(() => {
        getBatiments()
            .then(setBatiments)
            .catch(() => setError("Impossible de charger les bâtiments."));
    }, []);

    console.log(batiments);

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
            setBatiments([...batiments, nouveau]); // on l'ajoute à la liste affichée

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
            setBatiments(batiments.filter((b) => b.id !== id));
        } catch (err) {
            if (err instanceof BatimentError && err.status === 403) setError("Vous n'avez pas le droit de supprimer un bâtiment.");
            else if (err instanceof BatimentError && err.status === 404) setError("Ce bâtiment n'existe plus.");
            else if (err instanceof BatimentError && err.status === 409) setError("Ce bâtiment contient encore des salles.");
            else setError("Le service est indisponible. Réessayez plus tard.");
        }
    }

    return (
        <main>
            <h1>Gestion Batiment</h1>

            {!formOuvert && (
                <button onClick={() => setFormOuvert(true)}>Créer un bâtiment</button>
            )}

            {formOuvert && (
                <form onSubmit={handleSubmit} noValidate>
                    <label htmlFor="numBatiment">Numéro du bâtiment</label>
                    <input
                        id="numBatiment"
                        value={numBatiment}
                        onChange={(e) => setNumBatiment(e.target.value)}
                        autoFocus
                    />

                    {erreurForm && <p className="error-message" role="alert">{erreurForm}</p>}

                    <button type="button" onClick={fermerForm}>Annuler</button>
                    <button type="submit" disabled={envoi}>
                        {envoi ? "Création…" : "Créer"}
                    </button>
                </form>
            )}

            {error && <p className="error-message" role="alert">{error}</p>}

            <div>
                {batiments.map((b) => (
                    <BatimentCard batiment={b} onSupprime={handleSupprimer}  />
                ))}
            </div>

        </main>
    );
}
