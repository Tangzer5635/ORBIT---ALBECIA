import {useEffect, useState} from "react";
import Popup from "../Popup.tsx";
import {LIBELLE_TYPE_MATERIEL, type Materiel, type Poste, type SalleCarte} from "../../../types/types.ts";
import {affecterMateriel, deplacerMateriel, type Emplacement, MaterielError} from "../../../services/materielService.ts";
import {getPostesBySalle, getSallesDuBatiment} from "../../../services/salleService.ts";
import {useBatiments} from "../../../context/BatimentsContext.tsx";
import {estFormateur, nomPoste, numeroPoste} from "../../../utils/postes.ts";
import {estMaterielDeSalle, placeSurPoste} from "../../../utils/materiels.ts";

interface Props {
    materiel: Materiel;
    depuis?: Emplacement; // emplacement actuel (si on le connaît : il est alors grisé dans la liste)
    enStock?: boolean; // en stock : on l'affecte (POST) au lieu de le déplacer (PATCH)
    onClose: () => void;
    onFait: () => void;
}

const DANS_LA_SALLE = "salle";

// Destination : bâtiment -> salle de cours -> posé dans la salle ou sur un de ses postes
export default function DeplacerMaterielPopup({materiel, depuis, enStock = false, onClose, onFait}: Props) {
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
    const dejaLa = (d: string) => !!depuis && (d === DANS_LA_SALLE
        ? "salleId" in depuis && depuis.salleId === Number(salleId)
        : "posteId" in depuis && depuis.posteId === Number(d));

    // vidéoprojecteur et tableau : seulement dans la salle ; UC et écran : seulement sur un poste qui a encore la place
    const deSalle = estMaterielDeSalle(materiel.type);
    const postePlein = (p: Poste) => !placeSurPoste(p.materielsParType ?? {}, materiel.type);

    async function valider() {
        if (!destination) return;
        setEnCours(true);
        setErreur("");
        try {
            const vers: Emplacement = destination === DANS_LA_SALLE
                ? {salleId: Number(salleId)}
                : {posteId: Number(destination)};
            await (enStock ? affecterMateriel(materiel.id, vers) : deplacerMateriel(materiel.id, vers));
            onFait();
        } catch (err) {
            setErreur(err instanceof MaterielError && err.detail ? err.detail
                : enStock ? "Affectation impossible." : "Déplacement impossible.");
            setEnCours(false);
        }
    }

    return (
        <Popup
            open
            onClose={onClose}
            title={`${enStock ? "Affecter" : "Changer d'emplacement"} : ${LIBELLE_TYPE_MATERIEL[materiel.type]}`}
            description={`${materiel.modele} (${materiel.numSerie})`}
            confirmLabel={enStock ? "Affecter" : "Déplacer"}
            onConfirm={valider}
            loading={enCours}
            disabled={!destination}
        >
            <label htmlFor="batiment">Bâtiment</label>
            <select id="batiment" value={batimentId} onChange={e => choisirBatiment(e.target.value)}>
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
                {deSalle && (
                    <option value={DANS_LA_SALLE} disabled={dejaLa(DANS_LA_SALLE)}>Matériel de salle (posé dans la salle)
                    </option>
                )}
                {!deSalle && postes.map(p => (
                    <option key={p.id} value={p.id} disabled={dejaLa(String(p.id)) || postePlein(p)}>
                        {nomPoste(p)}{p.occupant ? `, ${p.occupant}` : ""}{!dejaLa(String(p.id)) && postePlein(p) ? " (complet)" : ""}
                    </option>
                ))}
            </select>

            {erreur && <p className="error-message" role="alert">{erreur}</p>}
        </Popup>
    );
}