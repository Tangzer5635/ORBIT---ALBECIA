import {useEffect, useState} from "react";
import Popup from "../Popup.tsx";
import type {Batiment, Master, SalleCarte} from "../../../types/types.ts";
import {getBatiments} from "../../../services/batimentService.ts";
import {getSallesDuBatiment} from "../../../services/salleService.ts";
import {MasterError, plaquerMaster} from "../../../services/masterService.ts";
import {libelleEtage, pluriel} from "../../../utils/postes.ts";

interface Props {
    master: Master;
    onClose: () => void;
    onFait: () => void;
}

// « Plaquer sur une salle » : choix du bâtiment puis d'une salle de cours non gelée
export default function PlaquerMasterPopup({master, onClose, onFait}: Props) {
    const [batiments, setBatiments] = useState<Batiment[]>([]);
    const [batimentId, setBatimentId] = useState("");
    const [salles, setSalles] = useState<SalleCarte[] | null>(null);
    const [salleId, setSalleId] = useState("");
    const [erreur, setErreur] = useState("");
    const [enCours, setEnCours] = useState(false);

    useEffect(() => {
        getBatiments().then(setBatiments).catch(() => setErreur("Impossible de charger les bâtiments."));
    }, []);

    useEffect(() => {
        if (!batimentId) return;
        getSallesDuBatiment(Number(batimentId))
            .then(liste => setSalles(liste.filter(s => s.type === "SalleDeCours")))
            .catch(() => setErreur("Impossible de charger les salles du bâtiment."));
    }, [batimentId]);

    async function valider() {
        if (!salleId) return;
        setEnCours(true);
        setErreur("");
        try {
            await plaquerMaster(Number(salleId), master.id);
            onFait();
        } catch (err) {
            setErreur(err instanceof MasterError && err.detail ? err.detail : "Impossible de plaquer le master.");
            setEnCours(false);
        }
    }

    return (
        <Popup
            open
            onClose={onClose}
            title={`Plaquer « ${master.nom} » sur une salle`}
            description={`Chaque UC de la salle reçoit les ${pluriel(master.processus.length, "processus", "processus")} du master : ses processus actuels sont remplacés.`}
            confirmLabel="Plaquer"
            onConfirm={valider}
            loading={enCours}
            disabled={!salleId}
        >
            <label htmlFor="plaquerBatiment">Bâtiment</label>
            <select id="plaquerBatiment" value={batimentId}
                    onChange={e => {
                        setBatimentId(e.target.value);
                        setSalleId("");
                        setSalles(null);
                    }}>
                <option value="" disabled>Choisir un bâtiment…</option>
                {batiments.map(b => <option key={b.id} value={b.id}>Bâtiment {b.numBatiment}</option>)}
            </select>

            {salles !== null && (salles.length === 0 ? (
                <p className="muted">Aucune salle de cours dans ce bâtiment.</p>
            ) : (
                <>
                    <label htmlFor="plaquerSalle">Salle</label>
                    <select id="plaquerSalle" value={salleId} onChange={e => setSalleId(e.target.value)}>
                        <option value="" disabled>Choisir une salle…</option>
                        {salles.map(s => (
                            <option key={s.id} value={s.id} disabled={s.suspendu}>
                                Salle {s.numSalle} ({libelleEtage(s.etage)}){s.suspendu ? " : gelée" : ""}
                            </option>
                        ))}
                    </select>
                </>
            ))}
            {erreur && <p className="error-message" role="alert">{erreur}</p>}
        </Popup>
    );
}