import {useEffect, useState} from "react";
import {Link} from "react-router-dom";
import Popup from "../Popup/Popup.tsx";
import ModifierMaterielPopup from "../Popup/Materiel/ModifierMaterielPopup.tsx";
import DeplacerMaterielPopup from "../Popup/Materiel/DeplacerMaterielPopup.tsx";
import {
    type Defaillance,
    LIBELLE_ETAT_MATERIEL,
    LIBELLE_STATUT_DEFAILLANCE,
    LIBELLE_TYPE_MATERIEL,
    type Materiel,
} from "../../types/types.ts";
import {type Emplacement, MaterielError, remettreEnStock, supprimerMateriel} from "../../services/materielService.ts";
import {getDefaillancesDuMateriel} from "../../services/defaillanceService.ts";
import {dateFr} from "../../utils/postes.ts";
import {CLASSE_ETAT_MATERIEL} from "../../utils/materiels.ts";

interface Props {
    materiel: Materiel;
    depuis?: Emplacement; // la salle ou le poste où il est posé (absent en lecture seule)
    onChange?: () => void; // le matériel a bougé ou changé : la page se recharge
    lecture?: boolean; // « Mon poste » du stagiaire : fiche sans actions
}

type Action = "modifier" | "deplacer" | "stock" | "supprimer" | null;

export default function MaterielCard({materiel: m, depuis, onChange = () => {}, lecture = false}: Props) {
    const [defaillance, setDefaillance] = useState<Defaillance | null>(null);
    const [action, setAction] = useState<Action>(null);
    const [erreur, setErreur] = useState("");
    const [enCours, setEnCours] = useState(false);

    // défaillance encore ouverte (la plus récente)
    useEffect(() => {
        getDefaillancesDuMateriel(m.id)
            .then(liste => setDefaillance(liste
                .filter(d => d.statut === "SIGNALEE")
                .sort((a, b) => b.dateSignalement.localeCompare(a.dateSignalement))[0] ?? null))
            .catch(console.error);
    }, [m.id]);

    const aujourdhui = new Date().toLocaleDateString("sv-SE"); // "2026-10-15"
    const garantieFinie = m.dateFinGarantie < aujourdhui;

    function fermer() {
        setAction(null);
        setErreur("");
    }

    async function confirmer(appel: () => Promise<unknown>, messageErreur: string) {
        setEnCours(true);
        setErreur("");
        try {
            await appel();
            setAction(null);
            onChange();
        } catch (err) {
            setErreur(err instanceof MaterielError && err.detail ? err.detail : messageErreur);
        } finally {
            setEnCours(false);
        }
    }

    const fait = () => {
        setAction(null);
        onChange();
    };

    return (
        <article className={`carte-mat ${CLASSE_ETAT_MATERIEL[m.etat]}`}>
            <header>
                <b>{LIBELLE_TYPE_MATERIEL[m.type]}</b>
                <span className="pastille">{LIBELLE_ETAT_MATERIEL[m.etat]}</span>
            </header>

            <dl>
                <dt>Modèle</dt>
                <dd>{m.modele}</dd>
                <dt>N° de série</dt>
                <dd className="mono">{m.numSerie}</dd>
                {m.nomMachine && (
                    <>
                        <dt>Nom machine</dt>
                        <dd className="mono">{m.nomMachine}</dd>
                    </>
                )}
                {m.adresseMac && (
                    <>
                        <dt>Adresse MAC</dt>
                        <dd className="mono">{m.adresseMac}</dd>
                    </>
                )}
                <dt>Acquis le</dt>
                <dd>{dateFr(m.dateAcquisition)}</dd>
                <dt>Garantie</dt>
                <dd>{garantieFinie ? "Terminée le " : "Jusqu'au "}{dateFr(m.dateFinGarantie)}</dd>
            </dl>

            {defaillance && (
                <div className="carte-defaillance">
                    <div>
                        <b>{defaillance.description}</b>
                        <span className="small">
                            {defaillance.signalePar}, le {dateFr(defaillance.dateSignalement)}.{" "}
                            {LIBELLE_STATUT_DEFAILLANCE[defaillance.statut]}
                        </span>
                    </div>
                    {!lecture && <Link to={`/defaillances?ouvrir=${defaillance.id}`} className="btn sm">Voir la fiche</Link>}
                </div>
            )}

            {!lecture && depuis && (
                <div className="carte-actions">
                    <button type="button" className="btn sm" onClick={() => setAction("modifier")}>Modifier</button>
                    <button type="button" className="btn sm" onClick={() => setAction("deplacer")}>Changer d'emplacement</button>
                    <button type="button" className="btn sm" onClick={() => setAction("stock")}>Remettre en stock</button>
                    <button type="button" className="btn sm danger" onClick={() => setAction("supprimer")}>Supprimer</button>
                </div>
            )}

            {action === "modifier" && <ModifierMaterielPopup materiel={m} onClose={fermer} onFait={fait}/>}
            {action === "deplacer" && depuis && <DeplacerMaterielPopup materiel={m} depuis={depuis} onClose={fermer} onFait={fait}/>}
            {action === "stock" && depuis && (
                <Popup
                    open
                    onClose={fermer}
                    title={`Remettre en stock : ${LIBELLE_TYPE_MATERIEL[m.type]}`}
                    description={`${m.modele} (${m.numSerie}) retourne dans la réserve du bâtiment.`}
                    confirmLabel="Remettre en stock"
                    loading={enCours}
                    onConfirm={() => confirmer(() => remettreEnStock(m.id, depuis), "Remise en stock impossible.")}>
                    {erreur && <p className="error-message" role="alert">{erreur}</p>}
                </Popup>
            )}
            {action === "supprimer" && (
                <Popup
                    open
                    onClose={fermer}
                    title={`Supprimer : ${LIBELLE_TYPE_MATERIEL[m.type]}`}
                    description={`${m.modele} (${m.numSerie}) est retiré de la salle puis supprimé définitivement.`}
                    confirmLabel="Supprimer"
                    variant="danger"
                    loading={enCours}
                    onConfirm={() => confirmer(() => supprimerMateriel(m.id), "Suppression impossible.")}>
                    {erreur && <p className="error-message" role="alert">{erreur}</p>}
                </Popup>
            )}
        </article>
    );
}