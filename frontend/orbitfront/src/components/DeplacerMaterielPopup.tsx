import {useEffect, useState} from "react";
import Popup from "./Popup.tsx";
import {LIBELLE_TYPE_MATERIEL, type Materiel, type Poste, type SalleCarte} from "../types/types.ts";
import {deplacerMateriel, type Emplacement, MaterielError} from "../services/materielService.ts";
import {getPostesBySalle, getSallesDuBatiment} from "../services/salleService.ts";
import {useBatiments} from "../context/BatimentsContext.tsx";
import {estFormateur, nomPoste, numeroPoste} from "../utils/postes.ts";

interface Props {
    materiel: Materiel;
    depuis: Emplacement; // emplacement actuel
    onClose: () => void;
    onFait: () => void;
}

const DANS_LA_SALLE = "salle";

// Destination : bâtiment -> salle de cours -> posé dans la salle ou sur un de ses postes
export default function DeplacerMaterielPopup({materiel, depuis, onClose, onFait}: Props) {
    const {batiments} = useBatiments();
    const [batimentId, setBatimentId] = useState("");
    const [salles, setSalles] = useState<SalleCarte[]>([]);
    const [salleId, setSalleId] = useState("");
    const [postes, setPostes] = useState<Poste[]>([]);
    const [destination, setDestination] = useState(""); // DANS_LA_SALLE ou id du poste
    const [erreur, setErreur] = useState("");
    const [enCours, setEnCours] = useState(false);

    useEffect(() => {
        if (!batimentId) return;
        getSallesDuBatiment(Number(batimentId))
            .then(liste => setSalles(liste
                .filter(s => s.type === "SalleDeCours")
                .sort((a, b) => a.numSalle.localeCompare(b.numSalle, "fr", {numeric: true}))))
            .catch(() => setErreur("Impossible de charger les salles."));
    }, [batimentId]);

    useEffect(() => {
        if (!salleId) return;
        getPostesBySalle(salleId)
            .then(liste => setPostes([...liste].sort((a, b) =>
                Number(estFormateur(b)) - Number(estFormateur(a)) || numeroPoste(a) - numeroPoste(b))))
            .catch(() => setErreur("Impossible de charger les postes."));
    }, [salleId]);

    // changer de bâtiment ou de salle vide les choix qui en dépendent
    function choisirBatiment(id: string) {
        setBatimentId(id);
        setSalles([]);
        choisirSalle("");
    }

    function choisirSalle(id: string) {
        setSalleId(id);
        setPostes([]);
        setDestination("");
    }

    // on ne "déplace" pas un matériel là où il est déjà
    const dejaLa = (d: string) => d === DANS_LA_SALLE
        ? "salleId" in depuis && depuis.salleId === Number(salleId)
        : "posteId" in depuis && depuis.posteId === Number(d);

    async function valider() {
        if (!destination) return;
        setEnCours(true);
        setErreur("");
        try {
            await deplacerMateriel(materiel.id, destination === DANS_LA_SALLE
                ? {salleId: Number(salleId)}
                : {posteId: Number(destination)});
            onFait();
        } catch (err) {
            setErreur(err instanceof MaterielError && err.detail ? err.detail : "Déplacement impossible.");
            setEnCours(false);
        }
    }

    return (
        <Popup
            open
            onClose={onClose}
            title={`Changer d'emplacement : ${LIBELLE_TYPE_MATERIEL[materiel.type]}`}
            description={`${materiel.modele} (${materiel.numSerie})`}
            confirmLabel="Déplacer"
            onConfirm={valider}
            loading={enCours}
            disabled={!destination}
        >
            <label htmlFor="batiment">Bâtiment</label>
            <select id="batiment" value={batimentId} onChange={e => choisirBatiment(e.target.value)} autoFocus>
                <option value="" disabled>Choisir un bâtiment…</option>
                {batiments.map(b => <option key={b.id} value={b.id}>Bâtiment {b.numBatiment}</option>)}
            </select>

            <label htmlFor="salle">Salle</label>
            <select id="salle" value={salleId} onChange={e => choisirSalle(e.target.value)} disabled={!batimentId}>
                <option value="" disabled>Choisir une salle…</option>
                {salles.map(s => <option key={s.id} value={s.id}>Salle {s.numSalle}</option>)}
            </select>

            <label htmlFor="destination">Emplacement</label>
            <select id="destination" value={destination} onChange={e => setDestination(e.target.value)}
                    disabled={!salleId}>
                <option value="" disabled>Choisir un emplacement…</option>
                <option value={DANS_LA_SALLE} disabled={dejaLa(DANS_LA_SALLE)}>Matériel de salle (posé dans la salle)
                </option>
                {postes.map(p => (
                    <option key={p.id} value={p.id} disabled={dejaLa(String(p.id))}>
                        {nomPoste(p)}{p.occupant ? `, ${p.occupant}` : ""}
                    </option>
                ))}
            </select>

            {erreur && <p className="error-message" role="alert">{erreur}</p>}
        </Popup>
    );
}