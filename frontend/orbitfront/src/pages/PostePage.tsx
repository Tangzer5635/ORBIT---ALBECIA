import {useEffect, useState} from "react";
import {
    type Defaillance,
    type EtatPoste,
    LIBELLE_STATUT_DEFAILLANCE,
    LIBELLE_TYPE_MATERIEL,
    type MonPoste,
} from "../types/types.ts";
import {getMyPoste} from "../services/posteService.ts";
import {getUtilisateur} from "../services/authService.ts";
import {getMesDefaillances, signalerDefaillance} from "../services/defaillanceService.ts";
import Popup from "../components/Popup/Popup.tsx";
import MaterielCard from "../components/Materiel/MaterielCard.tsx";
import {dateFr, etatPoste} from "../utils/postes.ts";

// "B2-S105-P04" -> "04"
const numeroCourt = (numPoste: string) => numPoste.match(/P(\d+)$/)?.[1] ?? numPoste;

// titre et explication du bandeau selon l'état du poste
const MESSAGES: Record<EtatPoste, [string, string]> = {
    UTILISE: ["Tout fonctionne", "Votre matériel fonctionne normalement."],
    DISPONIBLE: ["Tout fonctionne", "Votre matériel fonctionne normalement."],
    DEFECTUEUX: ["Défectueux", "Un matériel de votre poste est signalé défectueux, il est en cours de traitement."],
    A_CHANGER: ["Prévoir changement", "Un matériel de votre poste a plus de 2 ans : il sera bientôt remplacé."],
    A_COMPLETER: ["À compléter", "Il manque du matériel sur votre poste."],
};

export default function PostePage() {
    const [poste, setPoste] = useState<MonPoste | null>();
    const [defaillances, setDefaillances] = useState<Defaillance[]>([]);
    const [erreurChargement, setErreurChargement] = useState<string>("");
    const [signalementOuvert, setSignalementOuvert] = useState(false);
    const [materielSignale, setMaterielSignale] = useState<number | "">("");
    const [description, setDescription] = useState("");
    const [envoiSignalement, setEnvoiSignalement] = useState(false);
    const [erreurSignalement, setErreurSignalement] = useState("");

    useEffect(() => {
        getMyPoste()
            .then(setPoste)
            .catch(() => setErreurChargement("Impossible de charger le poste."));
        getMesDefaillances().then(setDefaillances).catch(console.error);
    }, []);

    if (erreurChargement) {
        return (
            <>
                <div className="sh"><h1>Mon poste</h1></div>
                <p className="error-message" role="alert">{erreurChargement}</p>
            </>
        );
    }

    if (poste === undefined) return <p className="muted">Chargement…</p>;

    // Alternatif 1 : aucun poste affecté
    if (poste === null) {
        return (
            <>
                <div className="sh"><h1>Mon poste</h1></div>
                <div className="empty">
                    <h2>Aucun poste ne vous est attribué</h2>
                    <p className="muted">Votre formateur ou le gestionnaire vous attribue un poste au début de la session.</p>
                </div>
            </>
        );
    }

    const etat = poste.etat ?? "UTILISE";
    const [titre, explication] = MESSAGES[etat];
    const numero = numeroCourt(poste.numPoste);
    const verrouille = getUtilisateur()?.verrouille ?? false; // stagiaire verrouillé : plus de signalement
    const ouvrirSignalement = () => {
        setMaterielSignale(poste.materiels[0]?.id ?? "");
        setDescription("");
        setErreurSignalement("");
        setSignalementOuvert(true);
    };
    const envoyerSignalement = async () => {
        if (materielSignale === "") return setErreurSignalement("Sélectionnez le matériel concerné.");
        if (description.trim().length < 5) return setErreurSignalement("Décrivez la défaillance en au moins 5 caractères.");
        setEnvoiSignalement(true);
        setErreurSignalement("");
        try {
            await signalerDefaillance(materielSignale, description);
            // le poste aussi : son état (et la couleur du plan) change avec la défaillance
            const [mes, posteAJour] = await Promise.all([getMesDefaillances(), getMyPoste()]);
            setDefaillances(mes);
            setPoste(posteAJour);
            setSignalementOuvert(false);
        } catch (e) {
            setErreurSignalement(e instanceof Error ? e.message : "Impossible de signaler cette défaillance.");
        } finally {
            setEnvoiSignalement(false);
        }
    };

    return (
        <>
            <section className={`hero hero-poste ${etatPoste(etat)?.classe ?? ""}`}>
                <div className="ring rond-poste">
                    <div><b>{numero}</b><span>poste</span></div>
                </div>
                <div>
                    <h1>{titre}</h1>
                    <p>
                        Salle {poste.numSalle}, Bâtiment {poste.numBatiment}
                        {poste.stageNom && `, stage ${poste.stageNom}`}. {explication}
                    </p>
                    {verrouille ? (
                        <p className="error-message" role="alert">
                            Votre compte a été verrouillé, vous ne pouvez plus remonter de défaillance.
                        </p>
                    ) : (
                        <button type="button" className="btn primary" onClick={ouvrirSignalement}
                                disabled={poste.materiels.length === 0}>
                            Remonter une défaillance
                        </button>
                    )}
                </div>
            </section>

            <div className="room-layout">
                <div>
                    <div className="sh"><h2>Mon matériel</h2></div>
                    {poste.materiels.length === 0
                        ? <p className="muted">Aucun matériel sur ce poste.</p>
                        : <div className="poste-materiels">
                            {poste.materiels.map(m => <MaterielCard key={m.id} materiel={m} lecture/>)}
                        </div>}
                </div>

                <div>
                    <div className="sh"><h2>Où se trouve mon poste</h2></div>
                    <div className="card-s">
                        <div className="mplan-salle">
                            <span className="mplan-formateur">Formateur</span>
                            <div className="mplan">
                                {(poste.plan ?? []).map(p => (
                                    <i key={p.numPoste}
                                       className={`${etatPoste(p.etat ?? undefined)?.classe ?? ""}${p.numPoste === poste.numPoste ? " me" : ""}`}
                                       title={p.numPoste === poste.numPoste ? "Mon poste" : undefined}/>
                                ))}
                            </div>
                        </div>
                        <p className="muted small">Votre poste est entouré. Le bureau du formateur est en haut.</p>
                    </div>

                    <div className="sh"><h2>Mes défaillances remontées</h2></div>
                    <div className="list">
                        {defaillances.length === 0 ? (
                            <p className="muted">Vous n'avez remonté aucune défaillance.</p>
                        ) : defaillances.map(d => (
                            <div key={d.id} className={`li ${d.statut === "SIGNALEE" ? "def" : "use"}`}>
                                <strong>{d.description}</strong>
                                <strong>{LIBELLE_STATUT_DEFAILLANCE[d.statut]}</strong>
                                <span>{LIBELLE_TYPE_MATERIEL[d.typeMateriel]}, le {dateFr(d.dateSignalement)}</span>
                                <span>{d.statut === "RESOLUE" && d.traitePar ? `Résolue par ${d.traitePar}` : ""}</span>
                            </div>
                        ))}
                    </div>
                </div>
            </div>
            <Popup open={signalementOuvert} onClose={() => !envoiSignalement && setSignalementOuvert(false)}
                   title="Remonter une défaillance"
                   description="Décrivez le problème constaté sur un matériel de votre poste. Le formateur et le gestionnaire pourront le traiter."
                   confirmLabel="Remonter la défaillance" onConfirm={() => void envoyerSignalement()}
                   loading={envoiSignalement} disabled={materielSignale === "" || description.trim().length < 5}>
                <label className="lbl" htmlFor="materiel-signale">Matériel concerné</label>
                <select id="materiel-signale" className="input" value={materielSignale}
                        onChange={e => setMaterielSignale(e.target.value ? Number(e.target.value) : "")}>
                    {poste.materiels.length === 0 && <option value="">Aucun matériel disponible</option>}
                    {poste.materiels.map(m => <option key={m.id} value={m.id}>{LIBELLE_TYPE_MATERIEL[m.type]} · {m.modele} · {m.numSerie}</option>)}
                </select>
                <label className="lbl" htmlFor="description-defaillance">Que se passe-t-il ?</label>
                <textarea id="description-defaillance" className="input" rows={3} maxLength={500}
                          placeholder="Décrivez la défaillance en une phrase" value={description}
                          onChange={e => setDescription(e.target.value)}/>
                <p className="muted small">{description.length}/500 caractères</p>
                {erreurSignalement && <p className="error-message" role="alert">{erreurSignalement}</p>}
            </Popup>
        </>
    );
}