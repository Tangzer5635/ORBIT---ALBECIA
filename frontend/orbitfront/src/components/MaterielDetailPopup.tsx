import {useEffect, useState} from "react";
import {Link} from "react-router-dom";
import Popup from "./Popup.tsx";
import ModifierMaterielPopup from "./ModifierMaterielPopup.tsx";
import DeplacerMaterielPopup from "./DeplacerMaterielPopup.tsx";
import {
    type Defaillance,
    LIBELLE_ETAT_MATERIEL,
    LIBELLE_STATUT_DEFAILLANCE,
    LIBELLE_TYPE_MATERIEL,
    type Materiel,
} from "../types/types.ts";
import {archiverMateriel, MaterielError, supprimerMateriel} from "../services/materielService.ts";
import {getDefaillancesDuMateriel} from "../services/defaillanceService.ts";
import {CLASSE_ETAT_MATERIEL, localisationDe} from "../utils/materiels.ts";
import {dateFr} from "../utils/postes.ts";

interface Props {
    materiel: Materiel;
    onClose: () => void;
    onChange: () => void; // le matériel a changé : la liste se recharge
}

type Action = "modifier" | "emplacement" | "archiver" | "supprimer" | null;

// Fiche d'un matériel (clic sur une ligne de la liste) : détails, défaillances en cours et actions
export default function MaterielDetailPopup({materiel: m, onClose, onChange}: Props) {
    const [action, setAction] = useState<Action>(null);
    const [erreur, setErreur] = useState("");
    const [enCours, setEnCours] = useState(false);
    const [defaillances, setDefaillances] = useState<Defaillance[] | null>(null);
    const [erreurDefaillances, setErreurDefaillances] = useState(false);

    const localisation = localisationDe(m);
    const archive = localisation === "ARCHIVE";
    const enStock = localisation === "STOCK";
    const aujourdhui = new Date().toLocaleDateString("sv-SE"); // "2026-10-15"

    // défaillances encore ouvertes, la plus récente d'abord
    useEffect(() => {
        let annule = false;
        getDefaillancesDuMateriel(m.id)
            .then(liste => {
                if (annule) return;
                setDefaillances(liste
                    .filter(d => d.statut === "SIGNALEE")
                    .sort((a, b) => b.dateSignalement.localeCompare(a.dateSignalement)));
            })
            .catch(() => {
                if (!annule) setErreurDefaillances(true);
            });
        return () => {
            annule = true;
        };
    }, [m.id]);

    // Échap ferme la fiche (quand aucune autre popup n'est ouverte par-dessus)
    useEffect(() => {
        if (action !== null) return;
        const onKey = (e: KeyboardEvent) => {
            if (e.key === "Escape") onClose();
        };
        document.addEventListener("keydown", onKey);
        document.body.style.overflow = "hidden";
        return () => {
            document.removeEventListener("keydown", onKey);
            document.body.style.overflow = "";
        };
    }, [action, onClose]);

    function fermerAction() {
        setAction(null);
        setErreur("");
    }

    const fait = () => {
        setAction(null);
        onChange();
        onClose();
    };

    async function confirmer(appel: () => Promise<unknown>, messageErreur: string) {
        setEnCours(true);
        setErreur("");
        try {
            await appel();
            fait();
        } catch (err) {
            setErreur(err instanceof MaterielError && err.detail ? err.detail : messageErreur);
        } finally {
            setEnCours(false);
        }
    }

    const emplacement = archive
        ? `Archivé le ${m.dateArchivage ? dateFr(m.dateArchivage) : "?"}`
        : enStock ? "En stock" : "Affecté";

    return (
        <>
            {action === null && (
                <div className="popup-overlay" onMouseDown={onClose}>
                    <div
                        className="popup materiel-detail"
                        role="dialog"
                        aria-modal="true"
                        aria-label={`${LIBELLE_TYPE_MATERIEL[m.type]} ${m.modele}`}
                        onMouseDown={(e) => e.stopPropagation()}
                    >
                        <div className="popup-handle"/>
                        <header className="popup-header">
                            <div>
                                <h2>{LIBELLE_TYPE_MATERIEL[m.type]} {m.modele}</h2>
                                <p className="popup-sous-titre">N° {m.numSerie}</p>
                            </div>
                            <button type="button" className="popup-close" onClick={onClose} aria-label="Fermer">×</button>
                        </header>

                        <div className="popup-body">
                            <section className="detail-section">
                                <h3>Emplacement</h3>
                                <dl className="detail-dl">
                                    <dt>Situation</dt>
                                    <dd>{emplacement}</dd>
                                    {!archive && (
                                        <>
                                            <dt>Salle</dt>
                                            <dd>{m.numSalle ?? "?"}</dd>
                                        </>
                                    )}
                                </dl>
                            </section>

                            <article className={`carte-mat ${archive ? "dispo" : CLASSE_ETAT_MATERIEL[m.etat]}`}>
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
                                    <dt>Acquis le</dt>
                                    <dd>{dateFr(m.dateAcquisition)}</dd>
                                    <dt>Garantie</dt>
                                    <dd>{m.dateFinGarantie < aujourdhui ? "Terminée le " : "Jusqu'au "}{dateFr(m.dateFinGarantie)}</dd>
                                </dl>
                            </article>

                            <section className="detail-section">
                                <h3>Défaillances en cours</h3>
                                {erreurDefaillances && (
                                    <p className="error-message" role="alert">Impossible de charger les défaillances.</p>
                                )}
                                {!erreurDefaillances && defaillances === null && <p className="small">Chargement…</p>}
                                {defaillances?.length === 0 && <p className="small">Aucune défaillance en cours.</p>}
                                {defaillances?.map(d => (
                                    <div key={d.id} className="carte-defaillance">
                                        <div>
                                            <b>{d.description}</b>
                                            <span className="small">
                                                {d.signalePar}, le {dateFr(d.dateSignalement)}.{" "}
                                                {LIBELLE_STATUT_DEFAILLANCE[d.statut]}
                                            </span>
                                        </div>
                                        <Link to="/defaillances" className="btn sm">Voir la fiche</Link>
                                    </div>
                                ))}
                            </section>
                        </div>

                        {!archive && (
                            <footer className="carte-actions">
                                <button type="button" className="btn sm" onClick={() => setAction("modifier")}>Modifier</button>
                                <button type="button" className="btn sm" onClick={() => setAction("emplacement")}>
                                    {enStock ? "Affecter" : "Changer d'emplacement"}
                                </button>
                                {enStock && (
                                    <button type="button" className="btn sm" onClick={() => setAction("archiver")}>Archiver</button>
                                )}
                                <button type="button" className="btn sm danger" onClick={() => setAction("supprimer")}>Supprimer</button>
                            </footer>
                        )}
                    </div>
                </div>
            )}

            {action === "modifier" && <ModifierMaterielPopup materiel={m} onClose={fermerAction} onFait={fait}/>}
            {action === "emplacement" && (
                <DeplacerMaterielPopup materiel={m} enStock={enStock} onClose={fermerAction} onFait={fait}/>
            )}
            {action === "archiver" && (
                <Popup
                    open
                    onClose={fermerAction}
                    title={`Archiver : ${LIBELLE_TYPE_MATERIEL[m.type]}`}
                    description={`${m.modele} (${m.numSerie}) ne sera plus utilisable ni modifiable. Il reste consultable dans « Archivés ».`}
                    confirmLabel="Archiver"
                    loading={enCours}
                    onConfirm={() => confirmer(() => archiverMateriel(m.id), "Archivage impossible.")}>
                    {erreur && <p className="error-message" role="alert">{erreur}</p>}
                </Popup>
            )}
            {action === "supprimer" && (
                <Popup
                    open
                    onClose={fermerAction}
                    title={`Supprimer : ${LIBELLE_TYPE_MATERIEL[m.type]}`}
                    description={`${m.modele} (${m.numSerie}) est retiré de son emplacement puis supprimé définitivement. Un matériel qui a déjà eu une défaillance ne peut pas être supprimé : archive-le.`}
                    confirmLabel="Supprimer"
                    variant="danger"
                    loading={enCours}
                    onConfirm={() => confirmer(() => supprimerMateriel(m.id), "Suppression impossible.")}>
                    {erreur && <p className="error-message" role="alert">{erreur}</p>}
                </Popup>
            )}
        </>
    );
}
