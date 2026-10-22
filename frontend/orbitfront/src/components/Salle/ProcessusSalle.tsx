import {useEffect, useState} from "react";
import {LIBELLE_LISTE_PROCESSUS, type ListeProcessus, type ProcessusSalle as ProcessusEnSalle, type Salle} from "../../types/types.ts";
import {
    getProcessusDeLaSalle,
    ProcessusError,
    reinitialiserProcessus,
    surchargerProcessus
} from "../../services/processusService.ts";
import {getUtilisateur} from "../../services/authService.ts";

type Props = {
    salle: Salle;
};

const LISTES: ListeProcessus[] = ["BLANCHE", "NOIRE"];
const court = (l: ListeProcessus) => l === "BLANCHE" ? "Blanche" : "Noire";

// Processus de la salle : liste globale, et liste qui s'applique ici (surcharge d'un formateur de la salle).
// Un formateur de la salle peut surcharger : son choix vaut pour toutes ses salles. La liste noire est arrêtée sur les postes.
export default function ProcessusSalle({salle}: Props) {
    const utilisateur = getUtilisateur();
    const peutSurcharger = utilisateur?.role === "FORMATEUR"
        && (salle.formateurs ?? []).some(f => f.id === utilisateur.id);

    const [processus, setProcessus] = useState<ProcessusEnSalle[] | null>(null);
    const [erreurChargement, setErreurChargement] = useState("");
    const [erreur, setErreur] = useState("");
    const [enCours, setEnCours] = useState<number | null>(null); // id du processus en cours d'enregistrement
    const [recherche, setRecherche] = useState("");
    const [version, setVersion] = useState(0); // +1 = recharger

    useEffect(() => {
        getProcessusDeLaSalle(salle.id)
            .then(liste => {
                setProcessus(liste);
                setErreurChargement("");
            })
            .catch(() => setErreurChargement("Impossible de charger les processus de la salle."));
    }, [salle.id, version]);

    async function agir(p: ProcessusEnSalle, action: () => Promise<void>) {
        setErreur("");
        setEnCours(p.id);
        try {
            await action();
            setVersion(v => v + 1);
        } catch (e) {
            setErreur(e instanceof ProcessusError && e.detail ? e.detail : "Modification impossible.");
        } finally {
            setEnCours(null);
        }
    }

    const q = recherche.trim().toLowerCase();
    const visibles = (processus ?? []).filter(p => p.nom.toLowerCase().includes(q));
    const nbNoirs = (processus ?? []).filter(p => p.liste === "NOIRE").length;

    return (
        <section className="card-s processus-salle">
            <h2>Processus de la salle</h2>
            <p className="muted small">
                La liste globale vient de la page Processus. Un formateur de la salle peut la surcharger : son choix
                s'applique dans toutes ses salles, y compris pour un processus du master (si deux formateurs de la salle
                ne sont pas d'accord, le dernier choix l'emporte). Un processus en liste noire ici est arrêté
                automatiquement dès qu'il est lancé sur un poste de la salle
                {processus && ` (${nbNoirs} en liste noire)`}.
            </p>

            {erreurChargement && <p className="error-message" role="alert">{erreurChargement}</p>}
            {!erreurChargement && processus === null && <p className="muted">Chargement des processus…</p>}

            {processus && (
                <>
                    <input className="search" type="search" placeholder="Rechercher un processus"
                           aria-label="Rechercher un processus de la salle" value={recherche}
                           onChange={e => setRecherche(e.target.value)}/>
                    {erreur && <p className="error-message" role="alert">{erreur}</p>}
                    <table className="tbl">
                        <thead>
                        <tr>
                            <th>Processus</th>
                            <th>Liste globale</th>
                            <th>Dans la salle</th>
                            {peutSurcharger && <th></th>}
                        </tr>
                        </thead>
                        <tbody>
                        {visibles.map(p => (
                            <tr key={p.id}>
                                <td className="mono">{p.nom}</td>
                                <td className="muted">{court(p.listeGlobale)}</td>
                                <td>
                                    {peutSurcharger ? (
                                        <div className="seg" role="group" aria-label={`Liste de ${p.nom} dans mes salles`}>
                                            {LISTES.map(l => (
                                                <button key={l} type="button" aria-pressed={p.liste === l}
                                                        disabled={enCours !== null}
                                                        onClick={() => p.liste !== l
                                                            && void agir(p, () => surchargerProcessus(p.id, l))}>
                                                    {court(l)}
                                                </button>
                                            ))}
                                        </div>
                                    ) : (
                                        <b className={p.liste === "NOIRE" ? "processus-noir" : undefined}>
                                            {LIBELLE_LISTE_PROCESSUS[p.liste]}
                                        </b>
                                    )}
                                    {p.surchargePar && (
                                        <small className="muted processus-surcharge">Surchargé par {p.surchargePar}</small>
                                    )}
                                </td>
                                {peutSurcharger && (
                                    <td className="n">
                                        {p.maSurcharge && (
                                            <button type="button" className="btn sm" disabled={enCours !== null}
                                                    title="Revenir à la liste globale dans toutes mes salles"
                                                    onClick={() => void agir(p, () => reinitialiserProcessus(p.id))}>
                                                Réinitialiser
                                            </button>
                                        )}
                                    </td>
                                )}
                            </tr>
                        ))}
                        {visibles.length === 0 && (
                            <tr>
                                <td colSpan={peutSurcharger ? 4 : 3} className="muted">Aucun processus ne correspond.</td>
                            </tr>
                        )}
                        </tbody>
                    </table>
                </>
            )}
        </section>
    );
}
