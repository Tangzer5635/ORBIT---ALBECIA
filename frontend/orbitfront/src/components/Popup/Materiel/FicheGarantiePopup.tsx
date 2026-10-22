import {useEffect, useState} from "react";
import {Link} from "react-router-dom";
import Popup from "../Popup.tsx";
import {
    type AffectationMateriel,
    type Defaillance,
    LIBELLE_TYPE_MATERIEL,
    type Materiel,
} from "../../../types/types.ts";
import {getAffectationMateriel, MaterielError, retirerEtArchiverMateriel} from "../../../services/materielService.ts";
import {getDefaillancesDuMateriel} from "../../../services/defaillanceService.ts";
import {dateFr, libelleEtage} from "../../../utils/postes.ts";

interface Props {
    materiel: Materiel;
    onClose: () => void;
    onChange: () => void; // matériel archivé : le calendrier se recharge
}

const JOUR = 86_400_000;

// nombre de jours entre aujourd'hui et la date (négatif si elle est passée)
function joursAvant(iso: string): number {
    const aujourdhui = new Date();
    aujourdhui.setHours(0, 0, 0, 0);
    const [a, m, j] = iso.slice(0, 10).split("-").map(Number);
    return Math.round((new Date(a, m - 1, j).getTime() - aujourdhui.getTime()) / JOUR);
}

// 130 -> "4 mois", 12 -> "12 j"
const duree = (jours: number) => jours < 60 ? `${jours} j` : `${Math.round(jours / 30)} mois`;

// durée de la garantie, de l'achat à la fin : "2 ans", "1 an et 6 mois", "18 mois" si moins d'un an et demi
function dureeGarantie(achat: string, fin: string): string {
    const [a1, m1] = achat.split("-").map(Number);
    const [a2, m2] = fin.split("-").map(Number);
    const mois = (a2 - a1) * 12 + (m2 - m1);
    if (mois < 12) return `${mois} mois`;
    const ans = Math.floor(mois / 12), reste = mois % 12;
    return `${ans} an${ans > 1 ? "s" : ""}${reste ? ` et ${reste} mois` : ""}`;
}

// "B201-S302-F" -> "Poste formateur", "B201-S302-P07" -> "Poste 7"
function emplacement(a: AffectationMateriel): string {
    if (!a.numPoste) return a.typeSalle === "SalleStockage" ? "Rangé en stock" : "Matériel de la salle";
    if (a.numPoste.endsWith("-F")) return "Poste formateur";
    return `Poste ${Number(a.numPoste.match(/P(\d+)$/)?.[1] ?? 0)}`;
}

// Fiche garantie d'un matériel (clic dans la liste d'un mois du calendrier), comme la maquette
export default function FicheGarantiePopup({materiel: m, onClose, onChange}: Props) {
    const [affectation, setAffectation] = useState<AffectationMateriel | null>(null);
    const [erreurAffectation, setErreurAffectation] = useState(false);
    const [defaillances, setDefaillances] = useState<Defaillance[]>([]);
    const [confirmation, setConfirmation] = useState(false); // « Archiver » cliqué : on demande confirmation
    const [archivage, setArchivage] = useState(false);
    const [erreurArchivage, setErreurArchivage] = useState("");

    useEffect(() => {
        let actif = true;
        getAffectationMateriel(m.id)
            .then(a => { if (actif) setAffectation(a); })
            .catch(() => { if (actif) setErreurAffectation(true); });
        // défaillances encore à traiter : affichées sous l'affectation s'il y en a
        getDefaillancesDuMateriel(m.id)
            .then(liste => { if (actif) setDefaillances(liste.filter(d => d.statut === "SIGNALEE")); })
            .catch(() => { /* la fiche reste utilisable sans cette section */ });
        return () => { actif = false; };
    }, [m.id]);

    // Échap ferme la fiche (sauf si la confirmation d'archivage est ouverte à sa place)
    useEffect(() => {
        if (confirmation) return;
        const onKey = (e: KeyboardEvent) => e.key === "Escape" && onClose();
        document.addEventListener("keydown", onKey);
        document.body.style.overflow = "hidden";
        return () => {
            document.removeEventListener("keydown", onKey);
            document.body.style.overflow = "";
        };
    }, [confirmation, onClose]);

    async function archiver() {
        setArchivage(true);
        setErreurArchivage("");
        try {
            await retirerEtArchiverMateriel(m.id);
            onChange(); // il disparaît du calendrier ; on le retrouve dans Matériel avec le filtre « Archivés »
            onClose();
        } catch (err) {
            setErreurArchivage(err instanceof MaterielError && err.detail ? err.detail : "Archivage impossible.");
            setArchivage(false);
        }
    }

    if (confirmation) {
        const ou = affectation && affectation.typeSalle === "SalleDeCours" && affectation.idSalle != null
            ? `${emplacement(affectation)} de la salle ${affectation.numSalle}${affectation.occupant ? ` (${affectation.occupant})` : ""}`
            : null;
        return (
            <Popup open onClose={() => !archivage && setConfirmation(false)}
                   title={`Archiver ${LIBELLE_TYPE_MATERIEL[m.type].toLowerCase()} ${m.modele}`}
                   description={`${ou ? `Il est encore affecté (${ou}) : il en sera retiré et remis en stock, puis archivé. ` : ""}Un matériel archivé n'est plus utilisable ni modifiable ; il reste consultable dans Matériel avec le filtre « Archivés ».`}
                   confirmLabel="Archiver" variant="danger"
                   loading={archivage} onConfirm={() => void archiver()}>
                {erreurArchivage && <p className="error-message" role="alert">{erreurArchivage}</p>}
            </Popup>
        );
    }

    const reste = joursAvant(m.dateFinGarantie);
    const hors = reste < 0;
    const bientot = !hors && reste <= 90;
    const classe = hors ? "def" : bientot ? "chg" : "use";
    const libelle = hors ? "Hors garantie" : bientot ? "Fin de garantie proche" : "Sous garantie";

    // part de la garantie déjà écoulée, pour la barre
    const total = Math.max(1, -joursAvant(m.dateAcquisition) + reste);
    const ecoule = Math.min(100, Math.max(0, Math.round(-joursAvant(m.dateAcquisition) / total * 100)));

    const a = affectation;
    const enSalle = a?.typeSalle === "SalleDeCours" && a.idSalle != null;
    const lienSalle = enSalle
        ? `/salles/${a.idSalle}/?${a.idPoste ? `poste=${a.idPoste}` : `materiel=${m.id}`}`
        : null;

    return (
        <div className="popup-overlay" onMouseDown={onClose}>
            <div className="popup fiche-garantie" role="dialog" aria-modal="true"
                 aria-label={`${LIBELLE_TYPE_MATERIEL[m.type]} ${m.modele}`}
                 onMouseDown={e => e.stopPropagation()}>
                <div className="popup-handle"/>
                <div className="f-tete">
                    <div className={`f-head ${classe}`}>
                        <div>
                            <h2>{LIBELLE_TYPE_MATERIEL[m.type]} {m.modele}</h2>
                            <p>Matériel n° {m.id}, n° de série <span className="mono">{m.numSerie}</span></p>
                        </div>
                        <span className="pastille">{libelle}</span>
                    </div>
                    <button type="button" className="popup-close" onClick={onClose} aria-label="Fermer">×</button>
                </div>

                <section className="f-sec">
                    <h3>Garantie</h3>
                    <dl className="f-kv">
                        <dt>Achat</dt><dd>{dateFr(m.dateAcquisition)}</dd>
                        <dt>Durée</dt><dd>{dureeGarantie(m.dateAcquisition, m.dateFinGarantie)}</dd>
                        <dt>Fin</dt>
                        <dd className={bientot ? "f-warn" : undefined}>
                            <b>{dateFr(m.dateFinGarantie)}</b>, {hors ? `terminée depuis ${duree(-reste)}` : `dans ${duree(reste)}`}
                        </dd>
                    </dl>
                    <div className={`gbar ${classe}`} role="img" aria-label={`${ecoule} % de la garantie écoulée`}>
                        <i style={{width: `${ecoule}%`}}/>
                    </div>
                    <p className="muted small">
                        {hors
                            ? "Une panne n'est plus prise en charge : la réparation ou le remplacement est à la charge du centre."
                            : bientot
                                ? "Une panne survenant avant cette date est encore prise en charge. Pensez à faire vérifier le matériel ou à prolonger la garantie."
                                : "Une panne est prise en charge par le fabricant."}
                    </p>
                </section>

                <section className="f-sec">
                    <h3>Affectation</h3>
                    {erreurAffectation && <p className="error-message" role="alert">Impossible de charger l'affectation.</p>}
                    {!erreurAffectation && !a && <p className="muted small">Chargement…</p>}
                    {a && a.idSalle == null && <p className="muted">Matériel archivé, il n'est plus affecté.</p>}
                    {a && a.idSalle != null && (
                        <dl className="f-kv">
                            {a.idPoste != null && !a.numPoste?.endsWith("-F") && (
                                <><dt>Affecté à</dt><dd><b>{a.occupant ?? "Personne, poste libre"}</b></dd></>
                            )}
                            <dt>{a.typeSalle === "SalleStockage" ? "Stockage" : "Salle"}</dt>
                            <dd>
                                Salle {a.numSalle}
                                {a.numBatiment && `, Bâtiment ${a.numBatiment}`}
                                {a.etage && `, ${libelleEtage(a.etage).toLowerCase()}`}
                            </dd>
                            <dt>Emplacement</dt><dd>{emplacement(a)}</dd>
                            {a.typeSalle === "SalleDeCours" && (
                                <>
                                    <dt>Stage</dt>
                                    <dd>{a.stageNom
                                        ? `${a.stageNom}${a.stageDateDebut && a.stageDateFin ? `, du ${dateFr(a.stageDateDebut)} au ${dateFr(a.stageDateFin)}` : ""}`
                                        : <span className="muted">Aucun stage</span>}</dd>
                                </>
                            )}
                            <dt>Responsable</dt><dd>{a.responsable ?? <span className="muted">Non renseigné</span>}</dd>
                        </dl>
                    )}
                </section>

                {defaillances.length > 0 && (
                    <section className="f-sec">
                        <h3>Défaillances à traiter ({defaillances.length})</h3>
                        <ul className="f-pbs">
                            {defaillances.map(d => (
                                <li key={d.id} className="open">
                                    <span>
                                        <b>{d.description}</b><br/>
                                        <small>{d.signalePar}, le {dateFr(d.dateSignalement)}{hors ? "" : ", réparation encore couverte"}</small>
                                    </span>
                                    <Link className="btn sm" to={`/defaillances?ouvrir=${d.id}`}>Voir la fiche</Link>
                                </li>
                            ))}
                        </ul>
                    </section>
                )}

                <footer className="fiche-foot">
                    {lienSalle && (
                        <Link className="btn" to={lienSalle}>{a?.idPoste ? "Voir le poste" : "Voir dans la salle"}</Link>
                    )}
                    {m.etat !== "ARCHIVE" && (
                        <button type="button" className="btn primary" onClick={() => setConfirmation(true)}>Archiver</button>
                    )}
                </footer>
            </div>
        </div>
    );
}
