import {useEffect, useState} from "react";
import {LIBELLE_TYPE_MATERIEL, type Materiel, type Poste, type Salle, type TypeMateriel} from "../../types/types.ts";
import {getMaterielById} from "../../services/materielService.ts";
import {signalerDefaillance} from "../../services/defaillanceService.ts";
import {getUtilisateur} from "../../services/authService.ts";
import MaterielCard from "../Materiel/MaterielCard.tsx";
import ProcessusLances from "./ProcessusLances.tsx";
import Popup from "../Popup/Popup.tsx";
import {estFormateur, etatPoste, nomPoste, pluriel} from "../../utils/postes.ts";
import {NOM_TYPE, ORDRE_TYPES} from "../../utils/materiels.ts";

type Props = {
    poste: Poste;
    salle: Salle;
    peutGerer: boolean; // gestionnaire ou admin : détail du matériel et actions sur le matériel
    // affecter / retirer un stagiaire : tout le rôle technique (formateur compris), donc toujours ouvert ici
    onFermer: () => void;
    onAffecterStagiaire?: () => void; // absent si la salle est gelée
    onRetirerStagiaire: () => void;
    onAffecterMateriel: () => void;
    onPlaquerMaster?: () => void; // absent si la salle est gelée ou pour qui ne gère pas la salle
    onChange: () => void; // un matériel a bougé : la page se recharge
};

export default function PanneauPoste({
                                         poste, salle, peutGerer, onFermer, onAffecterStagiaire, onRetirerStagiaire,
                                         onAffecterMateriel, onPlaquerMaster, onChange
                                     }: Props) {
    const [materiels, setMateriels] = useState<Materiel[] | null>(null);
    const [version, setVersion] = useState(0); // +1 après un signalement : recharge le matériel et ses défaillances
    const formateur = estFormateur(poste);
    const etat = etatPoste(poste.etat);
    // le formateur remonte une défaillance uniquement sur le poste formateur de ses salles
    const peutSignaler = formateur && getUtilisateur()?.role === "FORMATEUR";
    const voitMateriel = peutGerer || peutSignaler;

    const [signalementOuvert, setSignalementOuvert] = useState(false);
    const [materielSignale, setMaterielSignale] = useState<number | "">("");
    const [description, setDescription] = useState("");
    const [envoi, setEnvoi] = useState(false);
    const [erreurSignalement, setErreurSignalement] = useState("");

    // le détail du matériel : gestionnaire, ou formateur sur le poste formateur (sinon seulement les types)
    useEffect(() => {
        if (!voitMateriel) return;
        Promise.all(poste.materielsId.map(getMaterielById))
            .then(setMateriels)
            .catch(console.error);
    }, [poste, voitMateriel, version]);

    const formateurs = (salle.formateurs ?? []).map(f => `${f.prenom} ${f.nom}`);
    const typesPoses = ORDRE_TYPES
        .filter(t => poste.materielsParType?.[t])
        .map((t: TypeMateriel) => pluriel(poste.materielsParType[t] ?? 0, ...NOM_TYPE[t]));

    function ouvrirSignalement() {
        setMaterielSignale(materiels?.[0]?.id ?? "");
        setDescription("");
        setErreurSignalement("");
        setSignalementOuvert(true);
    }

    async function envoyerSignalement() {
        if (materielSignale === "") return setErreurSignalement("Sélectionnez le matériel concerné.");
        if (description.trim().length < 5) return setErreurSignalement("Décrivez la défaillance en au moins 5 caractères.");
        setEnvoi(true);
        setErreurSignalement("");
        try {
            await signalerDefaillance(materielSignale, description);
            setSignalementOuvert(false);
            setVersion(v => v + 1);
            onChange(); // l'état du poste passe à « Défectueux » sur le plan
        } catch (e) {
            setErreurSignalement(e instanceof Error ? e.message : "Impossible de signaler cette défaillance.");
        } finally {
            setEnvoi(false);
        }
    }

    return (
        <aside className="poste-panneau" aria-label={nomPoste(poste)}>
            <button type="button" className="muted small lien" onClick={onFermer}>‹ Vue de la salle</button>

            <div className={`panneau-entete ${etat?.classe ?? ""}`}>
                <div className="poste-entete">
                    <h2>{nomPoste(poste)}</h2>
                    {etat && <span className="pastille">{etat.libelle}</span>}
                </div>
                <p className="muted small">Salle {salle.numSalle}{salle.stageNom && `, ${salle.stageNom}`}</p>
            </div>

            {(peutGerer || peutSignaler || (!formateur && !poste.occupant && onAffecterStagiaire)) && (
                <div className="poste-actions">
                    {!formateur && !poste.occupant && onAffecterStagiaire && (
                        <button type="button" className="btn primary" onClick={onAffecterStagiaire}>
                            Affecter un stagiaire
                        </button>
                    )}
                    {peutSignaler && (
                        <button type="button" className="btn primary" onClick={ouvrirSignalement}
                                disabled={!materiels || materiels.length === 0}>
                            Remonter une défaillance
                        </button>
                    )}
                    {peutGerer && (
                        <button type="button" className="btn" onClick={onAffecterMateriel}>Affecter un matériel</button>
                    )}
                    {peutGerer && onPlaquerMaster && (
                        <button type="button" className="btn" onClick={onPlaquerMaster}>Plaquer un master sur ce poste</button>
                    )}
                </div>
            )}

            {formateur ? (
                <section className="panneau-section">
                    <h3>Utilisé par</h3>
                    <p className={formateurs.length ? "" : "muted"}>
                        {formateurs.length ? formateurs.join(", ") : "Aucun formateur attribué à la salle"}
                    </p>
                </section>
            ) : (
                <section className="panneau-section">
                    <h3>Occupant</h3>
                    {poste.occupant ? (
                        <ul className="panneau-liste">
                            <li>
                                {poste.occupant}
                                <button type="button" className="muted small lien" onClick={onRetirerStagiaire}>
                                    Retirer du poste
                                </button>
                            </li>
                        </ul>
                    ) : (
                        <p className="muted">Aucun stagiaire : poste disponible</p>
                    )}
                </section>
            )}

            <section className="panneau-section">
                <h3>Matériel du poste</h3>
                {poste.materielsId.length === 0 ? (
                    <p className="muted">Aucun matériel sur ce poste.</p>
                ) : !voitMateriel ? (
                    <p>{typesPoses.join(", ")}</p>
                ) : materiels === null ? (
                    <p className="muted">Chargement du matériel…</p>
                ) : (
                    // le formateur voit la fiche en lecture seule (pas de modification ni de déplacement)
                    materiels.map(m => (
                        <MaterielCard key={`${m.id}-${version}`} materiel={m} depuis={{posteId: poste.id}}
                                      onChange={onChange} lecture={!peutGerer}/>
                    ))
                )}
            </section>

            {/* processus de la machine du poste, lus sur l'UC par l'Agent ORBIT */}
            {(poste.materielsParType?.UC ?? 0) > 0 && <ProcessusLances posteId={poste.id}/>}

            {peutSignaler && (
                <Popup open={signalementOuvert} onClose={() => !envoi && setSignalementOuvert(false)}
                       title="Remonter une défaillance"
                       description="Décrivez le problème constaté sur un matériel du poste formateur. Le gestionnaire pourra le traiter."
                       confirmLabel="Remonter la défaillance" onConfirm={() => void envoyerSignalement()}
                       loading={envoi} disabled={materielSignale === "" || description.trim().length < 5}>
                    <label className="lbl" htmlFor="materiel-signale-formateur">Matériel concerné</label>
                    <select id="materiel-signale-formateur" className="input" value={materielSignale}
                            onChange={e => setMaterielSignale(e.target.value ? Number(e.target.value) : "")}>
                        {(materiels ?? []).map(m => (
                            <option key={m.id} value={m.id}>
                                {LIBELLE_TYPE_MATERIEL[m.type]} · {m.modele} · {m.numSerie}
                            </option>
                        ))}
                    </select>
                    <label className="lbl" htmlFor="description-defaillance-formateur">Que se passe-t-il ?</label>
                    <textarea id="description-defaillance-formateur" className="input" rows={3} maxLength={500}
                              placeholder="Décrivez la défaillance en une phrase" value={description}
                              onChange={e => setDescription(e.target.value)}/>
                    <p className="muted small">{description.length}/500 caractères</p>
                    {erreurSignalement && <p className="error-message" role="alert">{erreurSignalement}</p>}
                </Popup>
            )}
        </aside>
    );
}