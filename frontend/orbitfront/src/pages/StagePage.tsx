import {useEffect, useState} from "react";
import {LIBELLE_TYPE_FORMATION, type Stage, type Utilisateur} from "../types/types.ts";
import {getStages, getStagiaires, StageError, supprimerStage} from "../services/stageService.ts";
import GererStagiairesPopup from "../components/GererStagiairesPopup.tsx";
import Popup from "../components/Popup.tsx";
import ImporterStagesPopup from "../components/ImporterStagesPopup.tsx";
import {pluriel} from "../utils/postes.ts";
import {statutDe, type Statut} from "../utils/stages.ts";
import {useNavigate} from "react-router-dom";

const STATUTS: { statut: Statut; libelle: string; classe: string }[] = [
    {statut: "EN_COURS", libelle: "En cours", classe: "use"},
    {statut: "A_VENIR", libelle: "À venir", classe: "inc"},
    {statut: "TERMINE", libelle: "Terminés", classe: "dispo"},
];

const date = (iso: string) => new Date(iso).toLocaleDateString("fr-FR");

export default function StagePage() {
    const navigate = useNavigate();
    const [stages, setStages] = useState<Stage[]>([]);
    const [stagiaires, setStagiaires] = useState<Utilisateur[]>([]);
    const [erreurChargement, setErreurChargement] = useState("");
    const [filtre, setFiltre] = useState<Statut | null>(null);
    const [recherche, setRecherche] = useState("");
    const [stageOuvert, setStageOuvert] = useState<Stage | null>(null);
    const [importOuvert, setImportOuvert] = useState(false);
    const [stageASupprimer, setStageASupprimer] = useState<Stage | null>(null);
    const [erreurSuppression, setErreurSuppression] = useState("");
    const [suppressionEnCours, setSuppressionEnCours] = useState(false);
    const [version, setVersion] = useState(0); // +1 = recharger les stages (après un import)

    useEffect(() => {
        Promise.all([getStages(), getStagiaires()])
            .then(([s, u]) => {
                setStages(s);
                setStagiaires(u);
            })
            .catch(() => setErreurChargement("Impossible de charger les stages."));
    }, [version]);

    const nbStagiaires = (stage: Stage) => stagiaires.filter(s => s.stageId === stage.id).length;
    const nbParStatut = (statut: Statut) => stages.filter(s => statutDe(s) === statut).length;

    const visibles = stages.filter(s =>
        (filtre === null || statutDe(s) === filtre)
        && `${s.nom} ${LIBELLE_TYPE_FORMATION[s.type]} ${s.formateur}`.toLowerCase().includes(recherche.trim().toLowerCase()));

    // met à jour les stagiaires dont le stage a changé
    function majStagiaires(modifies: Utilisateur[]) {
        setStagiaires(liste => liste.map(s => modifies.find(m => m.id === s.id) ?? s));
    }

    function fermerSuppression() {
        setStageASupprimer(null);
        setErreurSuppression("");
    }

    // le back refuse un stage pas terminé, ou qui a encore des stagiaires ou une salle (message affiché dans la popup)
    async function confirmerSuppression() {
        if (!stageASupprimer) return;
        setSuppressionEnCours(true);
        setErreurSuppression("");
        try {
            await supprimerStage(stageASupprimer.id);
            fermerSuppression();
            setVersion(v => v + 1);
        } catch (err) {
            setErreurSuppression(err instanceof StageError && err.detail ? err.detail : "Suppression impossible.");
        } finally {
            setSuppressionEnCours(false);
        }
    }

    return (
        <>
            <div className="page-head">
                <h1>Stages</h1>
                <div className="seg" role="group" aria-label="Statut">
                    <button type="button" aria-pressed={filtre === null} onClick={() => setFiltre(null)}>
                        Tous ({stages.length})
                    </button>
                    {STATUTS.map(({statut, libelle}) => (
                        <button key={statut} type="button" aria-pressed={filtre === statut}
                                onClick={() => setFiltre(filtre === statut ? null : statut)}>
                            {libelle} ({nbParStatut(statut)})
                        </button>
                    ))}
                </div>
            </div>

            <button type="button" className="btn primary" onClick={() => setImportOuvert(true)}>
                Importer les stages (SAGAIE)
            </button>
            <p className="muted small">
                Les stages sont gérés dans SAGAIE et importés ici. Dans ORBIT, on gère leurs stagiaires et on les affecte aux salles, depuis la page de chaque salle.
            </p>

            {erreurChargement && <p className="error-message" role="alert">{erreurChargement}</p>}

            <input
                className="search"
                type="search"
                placeholder="Rechercher un stage, une formation, un formateur"
                aria-label="Rechercher un stage"
                value={recherche}
                onChange={(e) => setRecherche(e.target.value)}
            />

            <div className="list">
                {visibles.map(stage => {
                    const {libelle, classe} = STATUTS.find(x => x.statut === statutDe(stage))!;
                    return (
                        <div key={stage.id} className={`li ${classe}`}>
                            <strong>{stage.nom}</strong>
                            <strong>{libelle}</strong>

                            <span>
                                {LIBELLE_TYPE_FORMATION[stage.type]}, du {date(stage.dateDebut)} au {date(stage.dateFin)}, commandé par {stage.formateur}
                            </span>
                            <span>
                                {stage.salleId ? `Salle ${stage.numSalle}, Bâtiment ${stage.numBatiment}` : "Aucune salle"}, {pluriel(nbStagiaires(stage), "stagiaire")}
                            </span>

                            <span className="li-actions">
                                <button type="button" className="btn sm" disabled={!stage.salleId}
                                        onClick={() => navigate(`/salles/${stage.salleId}`)}>
                                    Voir la salle
                                </button>
                                <button type="button" className="btn sm" onClick={() => setStageOuvert(stage)}>
                                    Gérer les stagiaires
                                </button>
                                {statutDe(stage) === "TERMINE" && (
                                    <button type="button" className="btn sm danger" onClick={() => setStageASupprimer(stage)}>
                                        Supprimer
                                    </button>
                                )}
                            </span>
                        </div>
                    );
                })}
                {visibles.length === 0 && !erreurChargement && <p className="muted">Aucun stage.</p>}
            </div>

            {importOuvert && (
                <ImporterStagesPopup
                    onClose={() => setImportOuvert(false)}
                    onImporte={() => setVersion(v => v + 1)}/>
            )}

            {stageASupprimer && (
                <Popup
                    open
                    onClose={fermerSuppression}
                    title={`Supprimer le stage ${stageASupprimer.nom}`}
                    description="Seul un stage terminé peut être supprimé, et seulement s'il n'a plus de stagiaires ni de salle. Cette action est définitive."
                    confirmLabel="Supprimer"
                    variant="danger"
                    loading={suppressionEnCours}
                    onConfirm={confirmerSuppression}>
                    {erreurSuppression && <p className="error-message" role="alert">{erreurSuppression}</p>}
                </Popup>
            )}

            {stageOuvert && (
                <GererStagiairesPopup
                    stage={stageOuvert}
                    stagiaires={stagiaires}
                    onClose={() => setStageOuvert(null)}
                    onMaj={majStagiaires}
                />
            )}
        </>
    );
}