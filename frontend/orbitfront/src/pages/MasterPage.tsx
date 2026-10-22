import {useEffect, useState} from "react";
import {useNavigate} from "react-router-dom";
import type {Master, Salle} from "../types/types.ts";
import {getMasters, MasterError, supprimerMaster} from "../services/masterService.ts";
import {getSalle} from "../services/salleService.ts";
import {dateFr, pluriel} from "../utils/postes.ts";
import Popup from "../components/Popup/Popup.tsx";
import MasterPopup from "../components/Popup/Master/MasterPopup.tsx";
import PlaquerMasterPopup from "../components/Popup/Master/PlaquerMasterPopup.tsx";

// Masters : une carte par master (salles où il est plaqué, processus, actions)
export default function MasterPage() {
    const navigate = useNavigate();
    const [masters, setMasters] = useState<Master[]>([]);
    const [salles, setSalles] = useState<Salle[]>([]);
    const [erreurChargement, setErreurChargement] = useState("");
    const [version, setVersion] = useState(0); // +1 = recharger
    const [edition, setEdition] = useState<Master | "nouveau" | null>(null);
    const [aPlaquer, setAPlaquer] = useState<Master | null>(null);
    const [aSupprimer, setASupprimer] = useState<Master | null>(null);
    const [erreurSuppression, setErreurSuppression] = useState("");
    const [suppressionEnCours, setSuppressionEnCours] = useState(false);

    useEffect(() => {
        Promise.all([getMasters(), getSalle()])
            .then(([m, s]) => {
                setMasters(m);
                setSalles(s);
            })
            .catch(() => setErreurChargement("Impossible de charger les masters."));
    }, [version]);

    const recharger = () => setVersion(v => v + 1);
    const sallesDu = (m: Master) => salles.filter(s => s.masterId === m.id);
    // les masters déployés d'abord
    const tries = [...masters].sort((a, b) => sallesDu(b).length - sallesDu(a).length || a.nom.localeCompare(b.nom));
    const nbDeployes = masters.filter(m => sallesDu(m).length > 0).length;

    function fermerSuppression() {
        setASupprimer(null);
        setErreurSuppression("");
    }

    async function confirmerSuppression() {
        if (!aSupprimer) return;
        setSuppressionEnCours(true);
        setErreurSuppression("");
        try {
            await supprimerMaster(aSupprimer.id);
            fermerSuppression();
            recharger();
        } catch (err) {
            setErreurSuppression(err instanceof MasterError && err.detail ? err.detail : "Suppression impossible.");
        } finally {
            setSuppressionEnCours(false);
        }
    }

    const apres = () => {
        setEdition(null);
        setAPlaquer(null);
        recharger();
    };

    return (
        <>
            <div className="page-head">
                <h1>Masters</h1>
                <span className="muted">{pluriel(masters.length, "master")}, {pluriel(nbDeployes, "déployé")}</span>
            </div>

            <button type="button" className="btn primary" onClick={() => setEdition("nouveau")}>Créer un master</button>
            <p className="muted small">
                Un master regroupe les processus à installer sur les UC. On le plaque sur une salle entière : chaque UC
                de ses postes reçoit exactement ses processus. Un même master peut être plaqué sur plusieurs salles.
            </p>

            {erreurChargement && <p className="error-message" role="alert">{erreurChargement}</p>}

            <div className="rooms">
                {tries.map(m => {
                    const S = sallesDu(m);
                    return (
                        <article key={m.id} className="mcard">
                            <div className="mcard-tete">
                                <h2>{m.nom}</h2>
                                <span className={`f-st ${S.length ? "use" : "dispo"}`}>
                                    {S.length ? `Déployé sur ${pluriel(S.length, "salle")}` : "Non plaqué"}
                                </span>
                            </div>
                            <p className="muted">
                                Version {m.versionMaster}, créé le {dateFr(m.dateCreation)}, {pluriel(m.processus.length, "processus", "processus")}.
                            </p>
                            {S.length > 0 && (
                                <ul className="mrooms">
                                    {S.map(s => (
                                        <li key={s.id}>
                                            <button type="button" className="btn sm" onClick={() => navigate(`/salles/${s.id}`)}>
                                                Salle {s.numSalle}
                                            </button>
                                        </li>
                                    ))}
                                </ul>
                            )}
                            <ul className="chipl">
                                {m.processus.map(p => <li key={p.id} className="mono">{p.nom}</li>)}
                            </ul>
                            <div className="f-acts">
                                <button type="button" className="btn sm primary" onClick={() => setAPlaquer(m)}>
                                    Plaquer sur une salle
                                </button>
                                <button type="button" className="btn sm" onClick={() => setEdition(m)}>Modifier</button>
                                <button type="button" className="btn sm danger" onClick={() => setASupprimer(m)}>Supprimer</button>
                            </div>
                        </article>
                    );
                })}
            </div>
            {masters.length === 0 && !erreurChargement && <p className="muted">Aucun master.</p>}

            {edition && (
                <MasterPopup
                    master={edition === "nouveau" ? undefined : edition}
                    nbSalles={edition === "nouveau" ? 0 : sallesDu(edition).length}
                    onClose={() => setEdition(null)}
                    onFait={apres}/>
            )}
            {aPlaquer && <PlaquerMasterPopup master={aPlaquer} onClose={() => setAPlaquer(null)} onFait={apres}/>}
            {aSupprimer && (
                <Popup
                    open
                    onClose={fermerSuppression}
                    title={`Supprimer le master « ${aSupprimer.nom} »`}
                    description={sallesDu(aSupprimer).length
                        ? `Il est retiré des ${pluriel(sallesDu(aSupprimer).length, "salle")} où il est plaqué : leurs UC n'ont plus aucun processus. Cette action est définitive.`
                        : "Il n'est plaqué sur aucune salle. Cette action est définitive."}
                    confirmLabel="Supprimer"
                    variant="danger"
                    onConfirm={confirmerSuppression}
                    loading={suppressionEnCours}>
                    {erreurSuppression && <p className="error-message" role="alert">{erreurSuppression}</p>}
                </Popup>
            )}
        </>
    );
}