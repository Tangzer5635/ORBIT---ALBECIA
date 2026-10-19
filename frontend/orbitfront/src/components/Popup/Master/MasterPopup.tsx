import {useEffect, useState} from "react";
import Popup from "../Popup.tsx";
import type {Master, Processus} from "../../../types/types.ts";
import {creerMaster, MasterError, modifierMaster} from "../../../services/masterService.ts";
import {getProcessus} from "../../../services/processusService.ts";
import {pluriel} from "../../../utils/postes.ts";

interface Props {
    master?: Master; // absent = création
    nbSalles: number; // salles où il est plaqué (modification)
    onClose: () => void;
    onFait: () => void;
}

// « Créer un master » / « Modifier le master » : nom, version et processus à installer
export default function MasterPopup({master, nbSalles, onClose, onFait}: Props) {
    const [nom, setNom] = useState(master?.nom ?? "");
    const [version, setVersion] = useState(master?.versionMaster ?? "1.0");
    const [processus, setProcessus] = useState<Processus[]>([]);
    const [choisis, setChoisis] = useState<Set<number>>(new Set(master?.processusIds ?? []));
    const [erreur, setErreur] = useState("");
    const [enCours, setEnCours] = useState(false);

    useEffect(() => {
        getProcessus().then(setProcessus).catch(() => setErreur("Impossible de charger les processus."));
    }, []);

    function basculer(id: number) {
        setChoisis(avant => {
            const apres = new Set(avant);
            if (apres.has(id)) apres.delete(id);
            else apres.add(id);
            return apres;
        });
    }

    async function valider() {
        if (nom.trim().length < 3 || version.trim().length < 3) {
            setErreur("Le nom et la version font au moins 3 caractères.");
            return;
        }
        if (choisis.size === 0) {
            setErreur("Choisissez au moins un processus.");
            return;
        }
        setEnCours(true);
        setErreur("");
        const dto = {nom: nom.trim(), versionMaster: version.trim(), processusIds: [...choisis]};
        try {
            if (master) await modifierMaster(master.id, dto);
            else await creerMaster(dto);
            onFait();
        } catch (err) {
            setErreur(err instanceof MasterError && err.detail ? err.detail : "Impossible d'enregistrer le master.");
            setEnCours(false);
        }
    }

    return (
        <Popup
            open
            onClose={onClose}
            title={master ? "Modifier le master" : "Créer un master"}
            description="Les processus choisis seront installés sur chaque UC des salles où le master est plaqué."
            confirmLabel={master ? "Enregistrer" : "Créer le master"}
            onConfirm={valider}
            loading={enCours}
        >
            <label htmlFor="masterNom">Nom</label>
            <input id="masterNom" value={nom} onChange={e => setNom(e.target.value)} maxLength={50}
                   placeholder="Ex. Dev 2026" autoFocus/>

            <label htmlFor="masterVersion">Version</label>
            <input id="masterVersion" value={version} onChange={e => setVersion(e.target.value)} maxLength={50}/>

            <span className="popup-lbl" style={{marginTop: 12}}>Processus ({choisis.size})</span>
            <div className="pchks">
                {processus.map(p => {
                    const noir = p.liste === "NOIRE";
                    return (
                        <label key={p.id} className={`pchk${noir ? " no" : ""}`}
                               title={noir ? "En liste noire : interdit dans un master" : undefined}>
                            <input type="checkbox" checked={choisis.has(p.id) && !noir} disabled={noir}
                                   onChange={() => basculer(p.id)}/>
                            <span className="mono">{p.nom}{noir && " (liste noire)"}</span>
                        </label>
                    );
                })}
                {processus.length === 0 && <p className="muted small">Aucun processus dans le référentiel.</p>}
            </div>

            {master && nbSalles > 0 && (
                <p className="note-chg">
                    Les changements seront réinstallés sur les UC des {pluriel(nbSalles, "salle")} où ce master est plaqué.
                </p>
            )}
            {erreur && <p className="error-message" role="alert">{erreur}</p>}
        </Popup>
    );
}