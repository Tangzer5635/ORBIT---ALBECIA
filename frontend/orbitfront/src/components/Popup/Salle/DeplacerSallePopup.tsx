import {useEffect, useState} from "react";
import Popup from "../Popup.tsx";
import type {Batiment, Poste, Salle, SalleCarte} from "../../../types/types.ts";
import {getBatiments} from "../../../services/batimentService.ts";
import {deplacerSalle, getSallesDuBatiment, SalleError} from "../../../services/salleService.ts";
import {estFormateur, libelleEtage, pluriel} from "../../../utils/postes.ts";

interface Props {
    salle: Salle;
    postes: Poste[];
    onClose: () => void;
    onFait: (idCible: number) => void; // déplacement fait : la page ouvre la salle cible
}

// « Déplacer la salle » : choix de la salle cible, puis confirmation avant de tout déplacer
export default function DeplacerSallePopup({salle, postes, onClose, onFait}: Props) {
    const [batiments, setBatiments] = useState<Batiment[]>([]);
    const [batimentId, setBatimentId] = useState("");
    const [salles, setSalles] = useState<SalleCarte[] | null>(null);
    const [cibleId, setCibleId] = useState("");
    const [confirmation, setConfirmation] = useState(false);
    const [erreur, setErreur] = useState("");
    const [enCours, setEnCours] = useState(false);

    useEffect(() => {
        getBatiments().then(setBatiments).catch(() => setErreur("Impossible de charger les bâtiments."));
    }, []);

    // salles de cours du bâtiment choisi, sauf celle qu'on déplace
    useEffect(() => {
        if (!batimentId) return;
        getSallesDuBatiment(Number(batimentId))
            .then(liste => setSalles(liste.filter(s => s.type === "SalleDeCours" && s.id !== salle.id)))
            .catch(() => setErreur("Impossible de charger les salles du bâtiment."));
    }, [batimentId, salle.id]);

    const cible = salles?.find(s => s.id === Number(cibleId));
    const nbStagiaires = postes.filter(p => !estFormateur(p)).length;
    const nbVP = salle.materielsParType?.VideoProjecteur ?? 0;
    const nbTBI = salle.materielsParType?.TableauInteractif ?? 0;
    const nbFormateurs = salle.formateurs?.length ?? 0;

    async function deplacer() {
        if (!cible) return;
        setEnCours(true);
        setErreur("");
        try {
            await deplacerSalle(salle.id, cible.id);
            onFait(cible.id);
        } catch (err) {
            setErreur(err instanceof SalleError && err.detail ? err.detail : "Impossible de déplacer la salle.");
            setEnCours(false);
        }
    }

    if (confirmation && cible) {
        return (
            <Popup
                open
                onClose={onClose}
                title={`Déplacer la salle ${salle.numSalle} vers la salle ${cible.numSalle} ?`}
                description={`Le contenu actuel de la salle ${cible.numSalle} (postes, poste formateur et matériel) retourne au stock de son bâtiment. La salle ${salle.numSalle} restera vide.`}
                confirmLabel="Déplacer"
                variant="danger"
                cancelLabel="Retour"
                onCancel={() => {
                    setConfirmation(false);
                    setErreur("");
                }}
                onConfirm={deplacer}
                loading={enCours}
            >
                <span className="popup-lbl">Ce qui part dans la salle {cible.numSalle}</span>
                <ul>
                    <li>{pluriel(nbStagiaires, "poste")} stagiaire et le poste formateur, avec leur matériel</li>
                    {nbVP > 0 && <li>{pluriel(nbVP, "vidéoprojecteur")}</li>}
                    {nbTBI > 0 && <li>{pluriel(nbTBI, "tableau interactif", "tableaux interactifs")}</li>}
                    {salle.stageNom && <li>Le stage {salle.stageNom} et ses stagiaires installés</li>}
                    {nbFormateurs > 0 && <li>{pluriel(nbFormateurs, "formateur attribué", "formateurs attribués")}</li>}
                </ul>
                {erreur && <p className="error-message" role="alert">{erreur}</p>}
            </Popup>
        );
    }

    return (
        <Popup
            open
            onClose={onClose}
            title={`Déplacer la salle ${salle.numSalle}`}
            description="Tout le contenu de la salle (postes, matériel, stage, formateurs, master) part dans la salle choisie. Elle doit avoir au moins autant de postes stagiaires, de vidéoprojecteurs et de tableaux, et ne pas accueillir de stage."
            confirmLabel="Continuer"
            onConfirm={() => cible && setConfirmation(true)}
            disabled={!cible || cible.suspendu}
        >
            <label htmlFor="batimentCible">Bâtiment</label>
            <select id="batimentCible" value={batimentId} autoFocus
                    onChange={e => {
                        setBatimentId(e.target.value);
                        setCibleId("");
                        setSalles(null);
                    }}>
                <option value="" disabled>Choisir un bâtiment…</option>
                {batiments.map(b => <option key={b.id} value={b.id}>Bâtiment {b.numBatiment}</option>)}
            </select>

            {salles !== null && (salles.length === 0 ? (
                <p className="muted">Aucune autre salle de cours dans ce bâtiment.</p>
            ) : (
                <>
                    <label htmlFor="salleCible">Nouvelle salle</label>
                    <select id="salleCible" value={cibleId} onChange={e => setCibleId(e.target.value)}>
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