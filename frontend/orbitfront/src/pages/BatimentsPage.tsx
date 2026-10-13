import BatimentCard from "../components/BatimentCard.tsx";
import {useState} from "react";
import {ajouterBatiment, BatimentError, supprimerBatiment} from "../services/batimentService.ts";
import {useBatiments} from "../context/BatimentsContext.tsx";
import {ANNEAU_VIDE} from "../utils/postes.ts";
import Legende from "../components/Legende.tsx";
import Popup from "../components/Popup.tsx";
import type {Batiment} from "../types/types.ts";

type PopupState = { type: "creer" } | { type: "supprimer"; batiment: Batiment } | null;

// Le matériel est dans les salles ou les stockages : sans salle et stockages vides, il n'y en a pas
const aDuMateriel = (b: Batiment) => b.nbSalles > 0 || b.stockages.some((s) => s.nbMateriels > 0);

export function BatimentsPage() {
    const {batiments, setBatiments, erreurChargement} = useBatiments();

    const [popup, setPopup] = useState<PopupState>(null);
    const [numBatiment, setNumBatiment] = useState("");
    const [stockageCible, setStockageCible] = useState<number>();
    const [erreur, setErreur] = useState("");
    const [envoi, setEnvoi] = useState(false);

    function fermerPopup() {
        setPopup(null);
        setNumBatiment("");
        setStockageCible(undefined);
        setErreur("");
    }

    function ouvrirSuppression(batiment: Batiment) {
        // Présélectionne le premier stockage d'un autre bâtiment
        const premier = batiments.find((b) => b.id !== batiment.id)?.stockages[0];
        setStockageCible(premier?.id);
        setPopup({type: "supprimer", batiment});
    }

    async function handleCreer() {
        if (numBatiment.trim() === "") {
            setErreur("Le numéro du bâtiment est obligatoire.");
            return;
        }

        setEnvoi(true);
        try {
            const nouveau = await ajouterBatiment({numBatiment: numBatiment.trim()});
            setBatiments((liste) => [...liste, {...nouveau, anneau: ANNEAU_VIDE}]);
            fermerPopup();
        } catch (err) {
            if (err instanceof BatimentError && err.status === 403) setErreur("Vous n'avez pas le droit de créer un bâtiment.");
            else if (err instanceof BatimentError && err.status === 400) setErreur("Numéro de bâtiment invalide.");
            else setErreur("Le service est indisponible. Réessayez plus tard.");
        } finally {
            setEnvoi(false);
        }
    }

    async function handleSupprimer(batiment: Batiment) {
        const transfert = aDuMateriel(batiment);
        if (transfert && stockageCible === undefined) {
            setErreur("Choisissez une salle de stockage.");
            return;
        }

        setEnvoi(true);
        try {
            await supprimerBatiment(batiment.id, transfert ? stockageCible : undefined);
            setBatiments((liste) => liste.filter((b) => b.id !== batiment.id));
            fermerPopup();
        } catch (err) {
            if (err instanceof BatimentError && err.status === 403) setErreur("Vous n'avez pas le droit de supprimer un bâtiment.");
            else if (err instanceof BatimentError && err.status === 404) setErreur("Ce bâtiment n'existe plus.");
            else if (err instanceof BatimentError && err.status === 400) setErreur("Salle de stockage invalide.");
            else setErreur("Le service est indisponible. Réessayez plus tard.");
        } finally {
            setEnvoi(false);
        }
    }

    const nb = batiments.length;
    const nbSalles = batiments.reduce((acc, b) => acc + b.nbSalles, 0);
    const nbSallesStockage = batiments.reduce((acc, b) => acc + b.stockages.length, 0);

    // Bâtiment en cours de suppression (undefined si on est en création)
    const aSupprimer = popup?.type === "supprimer" ? popup.batiment : undefined;

    return (
        <>
            <div className="page-head">
                <h1>Bâtiments</h1>
                <span className="muted">{nb} bâtiment{nb > 1 ? "s" : ""}, {nbSalles} salles, {nbSallesStockage} salles de stockage</span>
            </div>

            <button type="button" className="btn primary" onClick={() => setPopup({type: "creer"})}>
                Créer un bâtiment
            </button>

            {erreurChargement && <p className="error-message" role="alert">{erreurChargement}</p>}
            <br/><br/>
            <Legende/>

            <div className="bat-grid">
                {batiments.map((b) => (
                    <BatimentCard key={b.id} batiment={b} onSupprime={ouvrirSuppression}/>
                ))}
            </div>

            <Popup
                open={popup !== null}
                onClose={fermerPopup}
                title={aSupprimer ? `Supprimer le Bâtiment ${aSupprimer.numBatiment} ?` : "Créer un bâtiment"}
                description={aSupprimer
                    ? `Ses salles seront supprimées${aDuMateriel(aSupprimer) ? " et son matériel déplacé vers la salle de stockage choisie" : ""}. Cette action est définitive.`
                    : "Le bâtiment est identifié par son numéro, par exemple « 4 » pour le Bâtiment 4."}
                confirmLabel={aSupprimer ? "Supprimer le bâtiment" : "Créer le bâtiment"}
                variant={aSupprimer ? "danger" : "primary"}
                onConfirm={aSupprimer ? () => handleSupprimer(aSupprimer) : handleCreer}
                loading={envoi}
            >
                {aSupprimer ? (
                    aDuMateriel(aSupprimer) && (
                        <>
                            <label htmlFor="stockageCible">Déplacer le matériel vers</label>
                            <select
                                id="stockageCible"
                                value={stockageCible ?? ""}
                                onChange={(e) => setStockageCible(Number(e.target.value))}
                            >
                                {batiments.filter((b) => b.id !== aSupprimer.id).map((b) => (
                                    <optgroup key={b.id} label={`Bâtiment ${b.numBatiment}`}>
                                        {b.stockages.map((s) => (
                                            <option key={s.id} value={s.id}>Salle {s.numSalle}</option>
                                        ))}
                                    </optgroup>
                                ))}
                            </select>
                        </>
                    )
                ) : (
                    <>
                        <label htmlFor="numBatiment">Numéro</label>
                        <input
                            id="numBatiment"
                            placeholder="Ex. 4"
                            value={numBatiment}
                            onChange={(e) => {
                                setNumBatiment(e.target.value);
                                setErreur("");
                            }}
                            autoFocus
                        />
                    </>
                )}

                {erreur && <p className="error-message" role="alert">{erreur}</p>}
            </Popup>
        </>
    );
}