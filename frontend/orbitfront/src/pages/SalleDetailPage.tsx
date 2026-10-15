import {Link, useParams} from "react-router-dom";
import {getPostesBySalle, getSalleById} from "../services/salleService.ts";
import {getMaterielById} from "../services/materielService.ts";
import {getUtilisateur} from "../services/authService.ts";
import {
    LIBELLE_ETAT_MATERIEL,
    type EtatPoste,
    type Formateur,
    type Materiel,
    type Poste,
    type Salle,
    type TypeMateriel,
} from "../types/types.ts";
import {useEffect, useState} from "react";
import Anneau from "../components/Anneau.tsx";
import {ANNEAU_VIDE, libelleEtage, pluriel} from "../utils/postes.ts";
import PanneauPoste from "../components/PanneauPoste.tsx";
import AffecterStagiairePopup from "../components/AffecterStagiairePopup.tsx";
import AffecterStagePopup from "../components/AffecterStagePopup.tsx";
import AffecterFormateurPopup from "../components/AffecterFormateurPopup.tsx";
import Popup from "../components/Popup.tsx";
import {retirerStageDeSalle} from "../services/stageService.ts";
import {retirerFormateurDeSalle} from "../services/formateurService.ts";

// ordre des tuiles, libellé et classe de couleur
const ETATS: { etat: EtatPoste; libelle: string; classe: string }[] = [
    {etat: "DEFECTUEUX", libelle: "Défectueux", classe: "def"},
    {etat: "A_COMPLETER", libelle: "À compléter", classe: "inc"},
    {etat: "A_CHANGER", libelle: "Prévoir changement", classe: "chg"},
    {etat: "UTILISE", libelle: "Utilisé", classe: "use"},
    {etat: "DISPONIBLE", libelle: "Disponible", classe: "dispo"},
];

// libellé court du matériel posé dans la salle (plan)
const COURT: Record<TypeMateriel, string> = {
    UC: "UC",
    Ecran: "Écran",
    VideoProjecteur: "VP",
    TableauInteractif: "TBI",
};

const classeEtat = (e?: EtatPoste) => ETATS.find(x => x.etat === e)?.classe ?? "";

// "B201-S302-F" = poste formateur, "B201-S302-P07" -> 7
const estFormateur = (p: Poste) => p.numPoste.endsWith("-F");
const numero = (p: Poste) => Number(p.numPoste.match(/P(\d+)$/)?.[1] ?? 0);

// regroupe les postes 2 par 2 (une table = 2 postes côte à côte)
function parTables(postes: Poste[]): Poste[][] {
    const tables: Poste[][] = [];
    for (let i = 0; i < postes.length; i += 2) tables.push(postes.slice(i, i + 2));
    return tables;
}

export default function SalleDetailPage() {
    const {salleId} = useParams();
    const [salle, setSalle] = useState<Salle | null>(null);
    const [postes, setPostes] = useState<Poste[]>([]);
    const [materielsSalle, setMaterielsSalle] = useState<Materiel[]>([]);
    const [filtre, setFiltre] = useState<EtatPoste | null>(null);
    const role = getUtilisateur()?.role;
    const peutGerer = role === "ADMINISTRATEUR" || role === "GESTIONNAIRE";
    const [posteId, setPosteId] = useState<number | null>(null);
    const [popupStagiaire, setPopupStagiaire] = useState(false);
    const [popupStage, setPopupStage] = useState<"affecter" | "retirer" | null>(null);
    const [erreurStage, setErreurStage] = useState("");
    const [popupFormateur, setPopupFormateur] = useState(false);
    const [formateurARetirer, setFormateurARetirer] = useState<Formateur | null>(null);
    const [erreurFormateur, setErreurFormateur] = useState("");
    const [rechargement, setRechargement] = useState(0); // +1 = recharger la salle et ses postes
    const [refuse, setRefuse] = useState(false); // salle introuvable ou non affectée au formateur

    useEffect(() => {
        if (!salleId) return;
        setFiltre(null);
        setPosteId(null);
        setPopupStagiaire(false);
        setPopupStage(null);
        setMaterielsSalle([]);
        setRefuse(false);
        getSalleById(salleId)
            .then(s => {
                setSalle(s);
                // VP, tableau… posés dans la salle (pas sur un poste) : leur état vient du back
                if (peutGerer) {
                    Promise.all(s.materielsId.map(getMaterielById))
                        .then(setMaterielsSalle)
                        .catch(console.error);
                }
            })
            .catch(e => {
                console.error(e);
                setRefuse(true);
            });
        getPostesBySalle(salleId).then(setPostes).catch(console.error);
    }, [salleId, peutGerer, rechargement]);

    const compte = (e: EtatPoste) => postes.filter(p => p.etat === e).length;

    if (!salle) {
        return refuse
            ? <p className="error-message" role="alert">Cette salle n'existe pas ou ne fait pas partie de tes salles.</p>
            : <p className="muted">Chargement de la salle…</p>;
    }

    const posteFormateur = postes.find(estFormateur);
    const stagiaires = postes.filter(p => !estFormateur(p)).sort((a, b) => numero(a) - numero(b));
    // 2 colonnes de tables : la moitié des tables à gauche, l'autre à droite
    const tables = parTables(stagiaires);
    const rangees: Poste[][][] = [];
    for (let i = 0; i < tables.length; i += 2) rangees.push(tables.slice(i, i + 2));

    // pour un formateur (pas d'accès au détail du matériel) : seulement le type, sans état
    const chips = peutGerer
        ? materielsSalle.map(m => ({id: m.id, type: m.type, etat: m.etat}))
        : Object.entries(salle.materielsParType ?? {}).flatMap(([type, n]) =>
            Array.from({length: n ?? 0}, (_, i) => ({
                id: `${type}-${i}`,
                type: type as TypeMateriel,
                etat: undefined
            })));

    const estompe = (p: Poste) => filtre !== null && p.etat !== filtre;

    const posteSelectionne = postes.find(p => p.id === posteId);
    const formateurs = salle.formateurs ?? [];
    const nomFormateurs = formateurs.length === 0 ? "Aucun formateur"
        : `${formateurs[0].prenom} ${formateurs[0].nom}${formateurs.length > 1 ? ` +${formateurs.length - 1}` : ""}`;

    return (
        <>
            <Link to={-1 as unknown as string} className="muted small">‹ Retour</Link>

            <section className="hero">
                <Anneau id={`salle-${salle.id}`} comptes={salle.anneau ?? ANNEAU_VIDE} grand/>
                <div>
                    <h1>Salle {salle.numSalle}</h1>
                    <p>
                        {libelleEtage(salle.etage)}, {pluriel(stagiaires.length, "poste")} stagiaire.
                        {salle.stageNom && ` Stage ${salle.stageNom}.`}
                    </p>

                    <div className="tuiles">
                        {ETATS.map(({etat, libelle, classe}) => (
                            <button key={etat} type="button" className={`tuile ${classe}`}
                                    aria-pressed={filtre === etat}
                                    onClick={() => setFiltre(filtre === etat ? null : etat)}>
                                <b>{compte(etat)}</b>
                                <span>{libelle}</span>
                            </button>
                        ))}
                    </div>
                    <p className="rleg-note">
                        Le cercle indique la part du matériel utilisable (comme sur la carte de la salle). Les cases
                        comptent les postes : clique dessus pour filtrer le plan.
                    </p>
                </div>
            </section>

            {peutGerer && (
                <div className="hero-row">
                    <button type="button" className="btn">Modifier la salle</button>
                    {salle.type === "SalleDeCours" && (salle.stageId
                        ? <button type="button" className="btn" onClick={() => setPopupStage("retirer")}>Retirer le stage</button>
                        : <button type="button" className="btn" onClick={() => setPopupStage("affecter")}>Affecter un stage</button>)}
                    {salle.type === "SalleDeCours" &&
                        <button type="button" className="btn" onClick={() => setPopupFormateur(true)}>Affecter un formateur</button>}
                    <button type="button" className="btn">Exporter la salle</button>
                    <button type="button" className="btn">Geler la salle</button>
                    <button type="button" className="btn danger">Supprimer la salle</button>
                </div>
            )}

            <section className="plan">
                <div className={`plan-corps${posteSelectionne ? " ouvert" : ""}`}>
                    <div>
                        <div className="plan-salle">
                            {/* haut de la salle : matériel de salle à gauche, poste formateur à droite */}
                            <div className="plan-haut">
                                <div className="plan-materiels">
                                    {chips.map(c => (
                                        <div key={c.id}
                                             className={`plan-mat ${c.etat === "DEFECTUEUX" ? "def" : c.etat === "A_REMPLACER" ? "chg" : "use"}`}>
                                            <b>{COURT[c.type]}</b>
                                            {c.etat && <span>{LIBELLE_ETAT_MATERIEL[c.etat]}</span>}
                                        </div>
                                    ))}
                                </div>
                                {posteFormateur && (
                                    <button type="button"
                                            className={`plan-poste formateur ${classeEtat(posteFormateur.etat)}${estompe(posteFormateur) ? " estompe" : ""}${posteFormateur.id === posteId ? " select" : ""}`}
                                            title="Poste formateur"
                                            aria-pressed={posteFormateur.id === posteId}
                                            onClick={() => setPosteId(posteFormateur.id)}>
                                        <b>Formateur</b>
                                        <span>{nomFormateurs}</span>
                                    </button>
                                )}
                            </div>

                            {/* postes stagiaires : rangées de 2 tables de 2 postes */}
                            {rangees.map((rangee, r) => (
                                <div key={r} className="plan-rangee">
                                    {rangee.map((table, t) => (
                                        <div key={t} className="plan-table">
                                            {table.map(p => (
                                                <button key={p.id} type="button"
                                                        className={`plan-poste ${classeEtat(p.etat)}${estompe(p) ? " estompe" : ""}${p.id === posteId ? " select" : ""}`}
                                                        title={`Poste ${p.numPoste}`}
                                                        aria-pressed={p.id === posteId}
                                                        onClick={() => setPosteId(p.id)}>
                                                    <b>{numero(p)}</b>
                                                    <span>{p.occupantId ? "Occupé" : "Libre"}</span>
                                                </button>
                                            ))}
                                        </div>
                                    ))}
                                </div>
                            ))}

                            {stagiaires.length === 0 &&
                                <p className="muted">Aucun poste stagiaire dans cette salle.</p>}
                        </div>

                        <ul className="rleg plan-legende">
                            {ETATS.map(({etat, libelle, classe}) => (
                                <li key={etat} className={classe}><i
                                    className={`sw ${classe}`}/>{libelle} ({compte(etat)})</li>
                            ))}
                        </ul>
                    </div>
                    {posteSelectionne && (
                        <PanneauPoste poste={posteSelectionne} salle={salle} numero={numero(posteSelectionne)}
                                      onFermer={() => setPosteId(null)}
                                      onAffecterStagiaire={() => setPopupStagiaire(true)}
                                      formateur={estFormateur(posteSelectionne)}
                                      onAffecterFormateur={peutGerer ? () => setPopupFormateur(true) : undefined}
                                      onRetirerFormateur={peutGerer ? setFormateurARetirer : undefined}/>)}</div>
            </section>
            {popupFormateur && (
                <AffecterFormateurPopup
                    salle={salle}
                    onClose={() => setPopupFormateur(false)}
                    onFait={() => {
                        setPopupFormateur(false);
                        setRechargement(n => n + 1);
                    }}/>
            )}
            {formateurARetirer && (
                <Popup
                    open
                    onClose={() => setFormateurARetirer(null)}
                    title={`Retirer ${formateurARetirer.prenom} ${formateurARetirer.nom}`}
                    description={`Ce formateur n'aura plus accès à la salle ${salle.numSalle}.`}
                    confirmLabel="Retirer"
                    variant="danger"
                    onConfirm={() => {
                        setErreurFormateur("");
                        retirerFormateurDeSalle(formateurARetirer.id, salle.id)
                            .then(() => {
                                setFormateurARetirer(null);
                                setRechargement(n => n + 1);
                            })
                            .catch(() => setErreurFormateur("Impossible de retirer le formateur."));
                    }}>
                    {erreurFormateur && <p className="error-message" role="alert">{erreurFormateur}</p>}
                </Popup>
            )}
            {popupStage === "affecter" && (
                <AffecterStagePopup
                    salle={salle}
                    onClose={() => setPopupStage(null)}
                    onFait={() => {
                        setPopupStage(null);
                        setRechargement(n => n + 1);
                    }}/>
            )}
            {popupStage === "retirer" && salle.stageId && (
                <Popup
                    open
                    onClose={() => setPopupStage(null)}
                    title={`Retirer le stage ${salle.stageNom}`}
                    description="Les postes de la salle redeviennent libres : plus aucun stagiaire n'y est installé."
                    confirmLabel="Retirer"
                    variant="danger"
                    onConfirm={() => {
                        setErreurStage("");
                        retirerStageDeSalle(salle.stageId!, salle.id)
                            .then(() => {
                                setPopupStage(null);
                                setRechargement(n => n + 1);
                            })
                            .catch(() => setErreurStage("Impossible de retirer le stage."));
                    }}>
                    {erreurStage && <p className="error-message" role="alert">{erreurStage}</p>}
                </Popup>
            )}
            {popupStagiaire && posteSelectionne && (
                <AffecterStagiairePopup
                    numPoste={numero(posteSelectionne)}
                    idsDejaPlaces={postes.flatMap(p => p.occupantId ? [p.occupantId] : [])}
                    onClose={() => setPopupStagiaire(false)}
                    onAffecte={(stagiaireId) => {
                        console.log(`TODO appel API : stagiaire ${stagiaireId} -> poste ${posteSelectionne.id}`);
                        setPopupStagiaire(false);
                    }}/>
            )}
        </>
    );
}