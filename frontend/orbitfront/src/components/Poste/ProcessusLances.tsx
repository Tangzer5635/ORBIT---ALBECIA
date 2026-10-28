import {useEffect, useState} from "react";
import Popup from "../Popup/Popup.tsx";
import type {ProcessusLance, ProcessusPoste} from "../../types/types.ts";
import {getProcessusLances, ProcessusLanceError, tuerProcessus} from "../../services/processusLanceService.ts";
import {pluriel} from "../../utils/postes.ts";

const RAFRAICHISSEMENT_MS = 5000;

type Props = {
    posteId: number;
};

const messageErreur = (e: unknown, defaut: string) =>
    e instanceof ProcessusLanceError && e.detail ? e.detail : defaut;

// l'agent donne la mémoire en % de la RAM : convertie en Mo quand on connaît la RAM de la machine
const memoire = (pct: number, ramGo?: number) =>
    ramGo ? `${Math.round(pct / 100 * ramGo * 1024)} Mo` : `${pct.toFixed(1)} %`;

// Bloc « Processus lancés » d'un poste (centre de la page salle) : la liste vient de l'Agent ORBIT de l'UC,
// rafraîchie toutes les 5 s tant que le poste est affiché.
// « Tuer » arrête toutes les instances du processus sur la machine.
export default function ProcessusLances({posteId}: Props) {
    const [etat, setEtat] = useState<ProcessusPoste | null>(null);
    const [erreur, setErreur] = useState("");
    const [version, setVersion] = useState(0); // +1 après un arrêt : recharge tout de suite
    const [aTuer, setATuer] = useState<ProcessusLance | null>(null);
    const [enCours, setEnCours] = useState(false);
    const [erreurArret, setErreurArret] = useState("");
    const [message, setMessage] = useState("");

    useEffect(() => {
        let actif = true; // le poste a changé ou le panneau est fermé : on ignore les réponses en retard
        const charger = () => getProcessusLances(posteId)
            .then(reponse => {
                if (!actif) return;
                setEtat(reponse);
                setErreur("");
            })
            .catch(e => {
                if (!actif) return;
                setEtat(null);
                setErreur(messageErreur(e, "Impossible de joindre l'agent du poste."));
            });
        void charger();
        const minuteur = window.setInterval(() => void charger(), RAFRAICHISSEMENT_MS);
        return () => {
            actif = false;
            window.clearInterval(minuteur);
        };
    }, [posteId, version]);

    function ouvrirArret(p: ProcessusLance) {
        setErreurArret("");
        setATuer(p);
    }

    async function confirmerArret() {
        if (!aTuer) return;
        setEnCours(true);
        setErreurArret("");
        try {
            const arretes = await tuerProcessus(posteId, aTuer.nom);
            setMessage(`${aTuer.nom} arrêté (${pluriel(arretes, "instance")}).`);
            setATuer(null);
            setVersion(v => v + 1);
        } catch (e) {
            setErreurArret(messageErreur(e, "Arrêt du processus impossible."));
        } finally {
            setEnCours(false);
        }
    }

    return (
        <>
            {erreur ? (
                <p className="error-message" role="alert">{erreur}</p>
            ) : etat === null ? (
                <p className="muted">Connexion à l'agent du poste…</p>
            ) : !etat.conforme ? (
                <p className="error-message" role="alert">
                    Matériel non conforme : l'UC du poste ({etat.nomMachine || "sans nom machine"}, {etat.adresseMac})
                    ne correspond pas à la machine qui répond ({etat.machineDetectee ?? "sans nom"}).
                    Corrigez le nom machine ou l'adresse MAC de l'UC.
                </p>
            ) : (
                <>
                    {etat.processus.length === 0 ? (
                        <p className="muted">Aucune application ouverte sur ce poste.</p>
                    ) : (
                        <div className="proc-table">
                            <div className="proc-ligne entete">
                                <span>Processus</span><span className="opt">CPU</span><span className="opt">Mémoire</span><span/>
                            </div>
                            {etat.processus.map(p => (
                                <div key={p.nom} className="proc-ligne">
                                    <span className="mono" title={p.nom}>{p.nom}</span>
                                    <span className="opt">{Math.round(p.cpu)} %</span>
                                    <span className="opt">{memoire(p.memoire, etat.ramGo)}</span>
                                    <button type="button" className="btn sm danger" onClick={() => ouvrirArret(p)}>
                                        Tuer
                                    </button>
                                </div>
                            ))}
                        </div>
                    )}
                    <p className="muted small">Liste mise à jour toutes les 5 secondes.</p>
                </>
            )}

            {message && <p className="muted small" role="status">{message}</p>}

            <Popup open={aTuer !== null} onClose={() => !enCours && setATuer(null)}
                   title={`Tuer ${aTuer?.nom ?? ""}`}
                   description={aTuer && `${aTuer.nom} (${pluriel(aTuer.nombre, "instance")}) sera arrêté sur ${etat?.machineDetectee ?? "le poste"}. Le travail non enregistré dans cette application sera perdu.`}
                   confirmLabel="Tuer le processus" variant="danger"
                   loading={enCours} onConfirm={() => void confirmerArret()}>
                {erreurArret && <p className="error-message" role="alert">{erreurArret}</p>}
            </Popup>
        </>
    );
}