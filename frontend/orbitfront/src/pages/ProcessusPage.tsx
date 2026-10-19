import {useEffect, useState} from "react";
import {LIBELLE_LISTE_PROCESSUS, type ListeProcessus, type Master, type Processus} from "../types/types.ts";
import {changerListe, getProcessus, ProcessusError, supprimerProcessus} from "../services/processusService.ts";
import {getMasters} from "../services/masterService.ts";
import {pluriel} from "../utils/postes.ts";
import Popup from "../components/Popup/Popup.tsx";
import AjouterProcessusPopup from "../components/Popup/Processus/AjouterProcessusPopup.tsx";

// Référentiel des processus (liste blanche / noire) et nombre de masters qui contiennent chacun
export default function ProcessusPage() {
    const [processus, setProcessus] = useState<Processus[]>([]);
    const [masters, setMasters] = useState<Master[]>([]);
    const [erreurChargement, setErreurChargement] = useState("");
    const [recherche, setRecherche] = useState("");
    const [version, setVersion] = useState(0); // +1 = recharger
    const [ajoutOuvert, setAjoutOuvert] = useState(false);
    const [aSupprimer, setASupprimer] = useState<Processus | null>(null);
    const [erreurSuppression, setErreurSuppression] = useState("");
    const [suppressionEnCours, setSuppressionEnCours] = useState(false);
    const [erreurListe, setErreurListe] = useState(""); // refus du back (processus dans un master)

    useEffect(() => {
        Promise.all([getProcessus(), getMasters()])
            .then(([p, m]) => {
                setProcessus(p);
                setMasters(m);
            })
            .catch(() => setErreurChargement("Impossible de charger les processus."));
    }, [version]);

    const recharger = () => setVersion(v => v + 1);
    const mastersAvec = (p: Processus) => masters.filter(m => m.processusIds.includes(p.id));
    const q = recherche.trim().toLowerCase();
    const visibles = processus.filter(p => `${p.nom} ${LIBELLE_LISTE_PROCESSUS[p.liste]}`.toLowerCase().includes(q));

    // la blanche a le dessus : le back refuse la liste noire pour un processus qui est dans un master
    async function basculerListe(p: Processus, liste: ListeProcessus) {
        if (p.liste === liste) return;
        setErreurListe("");
        try {
            const modifie = await changerListe(p.id, liste);
            setProcessus(avant => avant.map(x => x.id === modifie.id ? modifie : x));
        } catch (err) {
            setErreurListe(err instanceof ProcessusError && err.detail ? err.detail : "Impossible de changer la liste.");
        }
    }

    function fermerSuppression() {
        setASupprimer(null);
        setErreurSuppression("");
    }

    async function confirmerSuppression() {
        if (!aSupprimer) return;
        setSuppressionEnCours(true);
        setErreurSuppression("");
        try {
            await supprimerProcessus(aSupprimer.id);
            fermerSuppression();
            recharger();
        } catch (err) {
            setErreurSuppression(err instanceof ProcessusError && err.detail ? err.detail : "Suppression impossible.");
        } finally {
            setSuppressionEnCours(false);
        }
    }

    return (
        <>
            <div className="page-head">
                <h1>Processus</h1>
                <span className="muted">{pluriel(processus.length, "processus référencé", "processus référencés")}</span>
            </div>

            <button type="button" className="btn primary" onClick={() => setAjoutOuvert(true)}>Ajouter un processus</button>

            {erreurChargement && <p className="error-message" role="alert">{erreurChargement}</p>}

            <section className="card-s" style={{marginTop: 14}}>
                <h2>Référentiel des processus</h2>
                <p className="muted small">
                    Liste blanche : autorisé, peut faire partie d'un master. Liste noire : interdit sur tous les postes
                    et désinstallé des UC. La liste blanche a toujours le dessus : un processus qui fait partie d'un
                    master ne peut pas passer en liste noire. Les processus lancés sur les postes arriveront avec
                    l'Agent ORBIT.
                </p>
                <input
                    className="search"
                    type="search"
                    placeholder="Rechercher un processus"
                    aria-label="Rechercher un processus"
                    value={recherche}
                    onChange={e => setRecherche(e.target.value)}
                />
                {erreurListe && <p className="error-message" role="alert">{erreurListe}</p>}
                <table className="tbl">
                    <thead>
                    <tr>
                        <th>Processus</th>
                        <th>Liste</th>
                        <th className="n">Masters</th>
                        <th></th>
                    </tr>
                    </thead>
                    <tbody>
                    {visibles.map(p => (
                        <tr key={p.id}>
                            <td className="mono">{p.nom}</td>
                            <td>
                                <div className="seg" role="group" aria-label={`Liste de ${p.nom}`}>
                                    {(Object.keys(LIBELLE_LISTE_PROCESSUS) as ListeProcessus[]).map(l => (
                                        <button key={l} type="button" aria-pressed={p.liste === l}
                                                onClick={() => basculerListe(p, l)}>
                                            {l === "BLANCHE" ? "Blanche" : "Noire"}
                                        </button>
                                    ))}
                                </div>
                            </td>
                            <td className="n">{mastersAvec(p).length}</td>
                            <td className="n">
                                <button type="button" className="btn sm danger" onClick={() => setASupprimer(p)}>Supprimer</button>
                            </td>
                        </tr>
                    ))}
                    {visibles.length === 0 && (
                        <tr>
                            <td colSpan={4} className="muted">Aucun processus ne correspond.</td>
                        </tr>
                    )}
                    </tbody>
                </table>
            </section>

            {ajoutOuvert && (
                <AjouterProcessusPopup
                    onClose={() => setAjoutOuvert(false)}
                    onFait={() => {
                        setAjoutOuvert(false);
                        recharger();
                    }}/>
            )}
            {aSupprimer && (
                <Popup
                    open
                    onClose={fermerSuppression}
                    title={`Supprimer ${aSupprimer.nom}`}
                    description={`${mastersAvec(aSupprimer).length
                        ? `Il est retiré de ${pluriel(mastersAvec(aSupprimer).length, "master")} et`
                        : "Il est"} désinstallé de toutes les UC. Cette action est définitive.`}
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