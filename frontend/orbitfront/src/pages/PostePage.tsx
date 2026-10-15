import {useEffect, useState} from "react";
import {
    type Defaillance,
    type EtatPoste,
    LIBELLE_STATUT_DEFAILLANCE,
    LIBELLE_TYPE_MATERIEL,
    type MonPoste,
} from "../types/types.ts";
import {getMyPoste} from "../services/posteService.ts";
import {getMesDefaillances} from "../services/defaillanceService.ts";
import CarteMateriel from "../components/CarteMateriel.tsx";
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
                    {/* « Remonter une défaillance » : cas d'utilisation 3, à brancher plus tard */}
                    <button type="button" className="btn primary" disabled title="Bientôt disponible">
                        Remonter une défaillance
                    </button>
                </div>
            </section>

            <div className="room-layout">
                <div>
                    <div className="sh"><h2>Mon matériel</h2></div>
                    {poste.materiels.length === 0
                        ? <p className="muted">Aucun matériel sur ce poste.</p>
                        : <div className="poste-materiels">
                            {poste.materiels.map(m => <CarteMateriel key={m.id} materiel={m} lecture/>)}
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
        </>
    );
}