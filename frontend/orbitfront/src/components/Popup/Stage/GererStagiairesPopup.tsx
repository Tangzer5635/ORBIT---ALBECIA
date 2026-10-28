import {useMemo, useState} from "react";
import Popup from "../Popup.tsx";
import {LIBELLE_TYPE_FORMATION, type Stage, type Utilisateur} from "../../../types/types.ts";
import {ajouterAStage, StageError, supprimerAStage} from "../../../services/stageService.ts";

interface Props {
    stage: Stage;
    stagiaires: Utilisateur[];                // tous les stagiaires
    onClose: () => void;
    onMaj: (modifies: Utilisateur[]) => void; // stagiaires dont le stage a changé
}

const date = (iso: string) => new Date(iso).toLocaleDateString("fr-FR");

export default function GererStagiairesPopup({stage, stagiaires, onClose, onMaj}: Props) {
    // Un stagiaire d'un autre stage n'apparaît pas : on ne change pas un stagiaire de stage.
    // Ceux déjà dans le stage d'abord, puis par nom ; l'ordre ne bouge pas quand on coche ou décoche.
    const visibles = useMemo(
        () => stagiaires.filter(s => s.stageId === stage.id || s.stageId == null)
            .sort((a, b) => Number(b.stageId === stage.id) - Number(a.stageId === stage.id)
                || a.nom.localeCompare(b.nom, "fr") || a.prenom.localeCompare(b.prenom, "fr")),
        [stagiaires, stage.id]);
    const dansLeStage = useMemo(() => new Set(visibles.filter(s => s.stageId === stage.id).map(s => s.id)), [visibles, stage.id]);

    const [coches, setCoches] = useState(() => new Set(dansLeStage));
    const [recherche, setRecherche] = useState("");
    const [erreur, setErreur] = useState("");
    const [envoi, setEnvoi] = useState(false);

    const aAjouter = visibles.filter(s => coches.has(s.id) && !dansLeStage.has(s.id));
    const aRetirer = visibles.filter(s => !coches.has(s.id) && dansLeStage.has(s.id));

    const filtres = visibles.filter(s =>
        `${s.prenom} ${s.nom} ${s.login}`.toLowerCase().includes(recherche.trim().toLowerCase()));

    function basculer(id: number) {
        setCoches(avant => {
            const apres = new Set(avant);
            if (!apres.delete(id)) apres.add(id);
            return apres;
        });
    }

    // Rien n'est envoyé avant le clic sur « Enregistrer »
    async function enregistrer() {
        setErreur("");
        setEnvoi(true);
        const modifies: Utilisateur[] = [];
        try {
            for (const s of aRetirer) modifies.push(await supprimerAStage(s.id, stage.id));
            for (const s of aAjouter) modifies.push(await ajouterAStage(s.id, stage.id));
            onMaj(modifies);
            onClose();
        } catch (err) {
            onMaj(modifies); // ce qui est déjà enregistré reste affiché
            setErreur(err instanceof StageError && err.status === 400 && err.detail
                ? err.detail
                : "Le service est indisponible. Réessayez plus tard.");
        } finally {
            setEnvoi(false);
        }
    }

    return (
        <Popup
            open
            onClose={onClose}
            title={`Stagiaires du stage ${stage.nom}`}
            description={`${LIBELLE_TYPE_FORMATION[stage.type]}, du ${date(stage.dateDebut)} au ${date(stage.dateFin)}. Les informations du stage viennent de SAGAIE ; seuls les stagiaires se gèrent ici.`}
            confirmLabel="Enregistrer"
            onConfirm={enregistrer}
            loading={envoi}
            disabled={aAjouter.length === 0 && aRetirer.length === 0}
        >
            <label htmlFor="rechercheStagiaire">Stagiaires ({coches.size})</label>
            <input
                id="rechercheStagiaire"
                type="search"
                placeholder="Rechercher un stagiaire"
                value={recherche}
                onChange={(e) => setRecherche(e.target.value)}
                onKeyDown={(e) => e.key === "Enter" && e.preventDefault()} // Entrée ne doit pas enregistrer
                autoFocus
            />

            <div className="stagiaires-liste">
                {filtres.map(s => (
                    <label key={s.id} className="stagiaire-ligne">
                        <input type="checkbox" checked={coches.has(s.id)} onChange={() => basculer(s.id)}/>
                        <span>
                            <b>{s.nom} {s.prenom}</b>
                            <small>ID {s.id}, {s.login}</small>
                        </span>
                    </label>
                ))}
                {filtres.length === 0 && <p className="muted">Aucun stagiaire.</p>}
            </div>

            {erreur && <p className="error-message" role="alert">{erreur}</p>}
        </Popup>
    );
}