import {Link, useNavigate, useParams, useSearchParams} from "react-router-dom";
import {convertirEnStockage, degelerSalle, getPostesBySalle, getSalleById, supprimerSalle} from "../services/salleService.ts";
import {getMaterielById} from "../services/materielService.ts";
import {getUtilisateur} from "../services/authService.ts";
import {
    LIBELLE_ETAT_MATERIEL,
    LIBELLE_MOTIF_GEL,
    type EtatPoste,
    type Formateur,
    type Materiel,
    type Poste,
    type Salle,
    type TypeMateriel,
} from "../types/types.ts";
import {useEffect, useState} from "react";
import Anneau from "../components/Utils/Anneau.tsx";
import {
    ANNEAU_VIDE,
    dateFr,
    estFormateur,
    etatPoste,
    ETATS_POSTE,
    libelleEtage,
    nomCourt,
    nomPoste,
    numeroPoste,
    pluriel
} from "../utils/postes.ts";
import {CLASSE_ETAT_MATERIEL, NOM_TYPE} from "../utils/materiels.ts";
import PanneauSalle from "../components/Salle/PanneauSalle.tsx";
import ProcessusSalle from "../components/Salle/ProcessusSalle.tsx";
import PanneauPoste from "../components/Poste/PanneauPoste.tsx";
import PanneauMateriel from "../components/Materiel/PanneauMateriel.tsx";
import AffecterStagiairePopup from "../components/Popup/Stage/AffecterStagiairePopup.tsx";
import AffecterMaterielPopup from "../components/Popup/Materiel/AffecterMaterielPopup.tsx";
import AffecterStagePopup from "../components/Popup/Stage/AffecterStagePopup.tsx";
import AffecterFormateurPopup from "../components/Popup/Salle/AffecterFormateurPopup.tsx";
import ModifierSallePopup from "../components/Popup/Salle/ModifierSallePopup.tsx";
import GelerSallePopup from "../components/Popup/Salle/GelerSallePopup.tsx";
import DeplacerSallePopup from "../components/Popup/Salle/DeplacerSallePopup.tsx";
import PlaquerMasterSallePopup from "../components/Popup/Master/PlaquerMasterSallePopup.tsx";
import Popup from "../components/Popup/Popup.tsx";
import {retirerStagiaireDuPoste} from "../services/posteService.ts";
import {retirerStageDeSalle} from "../services/stageService.ts";
import {retirerFormateurDeSalle} from "../services/formateurService.ts";

// libellé court du matériel posé dans la salle (plan)
const COURT: Record<TypeMateriel, string> = {
    UC: "UC",
    Ecran: "Écran",
    VideoProjecteur: "VP",
    TableauInteractif: "TBI",
};

const classeEtat = (e?: EtatPoste) => etatPoste(e)?.classe ?? "";

// poste sans stagiaire : « Libre » seulement s'il peut en recevoir un (ni défectueux, ni à compléter)
const libreOuEtat = (p: Poste) =>
    p.etat === "DEFECTUEUX" || p.etat === "A_COMPLETER" ? etatPoste(p.etat)?.libelle ?? "" : "Libre";

// regroupe les postes 2 par 2 (une table = 2 postes côte à côte)
function parTables(postes: Poste[]): Poste[][] {
    const tables: Poste[][] = [];
    for (let i = 0; i < postes.length; i += 2) tables.push(postes.slice(i, i + 2));
    return tables;
}

type PopupOuverte = "stagiaire" | "retirerStagiaire" | "materiel" | "stage" | "retirerStage" | "formateur"
    | "modifier" | "geler" | "deplacer" | "supprimer" | "master" | "masterPoste" | "convertir" | null;

// key = salleId : changer de salle repart d'un état propre (filtre, sélection, popups)
export default function SalleDetailPage() {
    const {salleId} = useParams();
    return salleId ? <SalleDetail key={salleId} salleId={salleId}/> : null;
}

function SalleDetail({salleId}: { salleId: string }) {
    const navigate = useNavigate();
    const [salle, setSalle] = useState<Salle | null>(null);
    const [postes, setPostes] = useState<Poste[]>([]);
    const [materielsSalle, setMaterielsSalle] = useState<Materiel[]>([]);
    const [filtre, setFiltre] = useState<EtatPoste | null>(null);
    const role = getUtilisateur()?.role;
    const peutGerer = role === "ADMINISTRATEUR" || role === "GESTIONNAIRE";
    // élément choisi sur le plan (rien = « Vue de la salle »)
    // ?poste=12 ou ?materiel=34 : arrive sur la salle avec ce poste ou ce matériel déjà sélectionné (fiche garantie)
    const [params] = useSearchParams();
    const [posteId, setPosteId] = useState<number | null>(() => Number(params.get("poste")) || null);
    const [materielId, setMaterielId] = useState<number | null>(() => Number(params.get("materiel")) || null);
    const [popup, setPopup] = useState<PopupOuverte>(null);
    const [erreurStage, setErreurStage] = useState("");
    const [formateurARetirer, setFormateurARetirer] = useState<Formateur | null>(null);
    const [erreurFormateur, setErreurFormateur] = useState("");
    const [erreurAction, setErreurAction] = useState(""); // erreur de la popup de confirmation ouverte
    const [info, setInfo] = useState(""); // message après « Modifier la salle » (matériel manquant)
    const [rechargement, setRechargement] = useState(0); // +1 = recharger la salle et ses postes
    const [refuse, setRefuse] = useState(false); // salle introuvable ou non affectée au formateur

    useEffect(() => {
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

    const recharger = () => setRechargement(n => n + 1);
    const fermerPopup = () => {
        setPopup(null);
        setErreurAction("");
    };

    // action d'une popup de confirmation : erreur affichée dans la popup si le back refuse
    function confirmer(action: () => Promise<unknown>, apres = apresAction) {
        setErreurAction("");
        action()
            .then(apres)
            .catch(err => setErreurAction(err?.detail || "L'action a échoué."));
    }
    const apresAction = () => {
        setPopup(null);
        recharger();
    };

    function choisirPoste(id: number) {
        setPosteId(id);
        setMaterielId(null);
    }

    function choisirMateriel(id: number) {
        setMaterielId(id);
        setPosteId(null);
    }

    function fermerPanneau() {
        setPosteId(null);
        setMaterielId(null);
    }

    const compte = (e: EtatPoste) => postes.filter(p => p.etat === e).length;

    if (!salle) {
        return refuse
            ? <p className="error-message" role="alert">Cette salle n'existe pas ou ne fait pas partie de tes salles.</p>
            : <p className="muted">Chargement de la salle…</p>;
    }

    const posteFormateur = postes.find(estFormateur);
    const stagiaires = postes.filter(p => !estFormateur(p)).sort((a, b) => numeroPoste(a) - numeroPoste(b));
    // 2 colonnes de tables : la moitié des tables à gauche, l'autre à droite
    const tables = parTables(stagiaires);
    const rangees: Poste[][][] = [];
    for (let i = 0; i < tables.length; i += 2) rangees.push(tables.slice(i, i + 2));

    const estompe = (p: Poste) => filtre !== null && p.etat !== filtre;

    const posteSelectionne = postes.find(p => p.id === posteId);
    const materielSelectionne = materielsSalle.find(m => m.id === materielId);
    const formateurs = salle.formateurs ?? [];
    const nomFormateurs = formateurs.length === 0 ? "Aucun formateur"
        : `${formateurs[0].prenom} ${formateurs[0].nom}${formateurs.length > 1 ? ` +${formateurs.length - 1}` : ""}`;
    const salleDeCours = salle.type === "SalleDeCours";

    // panneau de droite : le poste ou le matériel choisi, sinon la vue de la salle
    const panneau = posteSelectionne ? (
        <PanneauPoste key={posteSelectionne.id} poste={posteSelectionne} salle={salle} peutGerer={peutGerer}
                      onFermer={fermerPanneau}
                      onAffecterStagiaire={salle.suspendu ? undefined : () => setPopup("stagiaire")}
                      onRetirerStagiaire={() => setPopup("retirerStagiaire")}
                      onAffecterMateriel={() => setPopup("materiel")}
                      onPlaquerMaster={peutGerer && salleDeCours && !salle.suspendu ? () => setPopup("masterPoste") : undefined}
                      onChange={recharger}/>
    ) : materielSelectionne ? (
        <PanneauMateriel key={materielSelectionne.id} materiel={materielSelectionne} salle={salle}
                         onFermer={fermerPanneau} onChange={recharger}/>
    ) : (
        <PanneauSalle salle={salle}
                      onAffecterStage={peutGerer && salleDeCours && !salle.suspendu ? () => setPopup("stage") : undefined}
                      onRetirerStage={peutGerer && salleDeCours ? () => setPopup("retirerStage") : undefined}
                      onAffecterFormateur={peutGerer && salleDeCours ? () => setPopup("formateur") : undefined}
                      onRetirerFormateur={peutGerer ? setFormateurARetirer : undefined}
                      onPlaquerMaster={peutGerer && salleDeCours && !salle.suspendu ? () => setPopup("master") : undefined}
        />
    );

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
                        {ETATS_POSTE.map(({etat, libelle, classe}) => (
                            <button key={etat} type="button" className={`tuile ${classe}`}
                                    aria-pressed={filtre === etat}
                                    onClick={() => setFiltre(filtre === etat ? null : etat)}>
                                <b>{compte(etat)}</b>
                                <span>{libelle}</span>
                            </button>
                        ))}
                    </div>
                </div>
            </section>

            {salle.suspendu && (
                <div className="note-gel">
                    <p>
                        <b>Salle gelée{salle.gelMotif && ` : ${LIBELLE_MOTIF_GEL[salle.gelMotif]}`}</b>
                        {salle.gelNote && `, ${salle.gelNote}`}.
                        {salle.gelDepuis && ` Depuis le ${dateFr(salle.gelDepuis)}`}
                        {salle.gelJusqu && `, jusqu'au ${dateFr(salle.gelJusqu)} prévu`}.
                        {" "}On ne peut plus y affecter de stage, de stagiaire ni de master.
                    </p>
                    {peutGerer && (
                        <button type="button" className="btn sm primary"
                                onClick={() => degelerSalle(salle.id).then(recharger).catch(() => setInfo("Impossible de dégeler la salle."))}>
                            Dégeler la salle
                        </button>
                    )}
                </div>
            )}

            {peutGerer && salleDeCours && (
                <div className="hero-row">
                    <button type="button" className="btn" onClick={() => setPopup("modifier")}>Modifier la salle</button>
                    {!salle.suspendu &&
                        <button type="button" className="btn" onClick={() => setPopup("deplacer")}>Déplacer la salle</button>}
                    {!salle.suspendu &&
                        <button type="button" className="btn" onClick={() => setPopup("geler")}>Geler la salle</button>}
                    {!salle.suspendu &&
                        <button type="button" className="btn" onClick={() => setPopup("convertir")}>Convertir en salle de stockage</button>}
                    <button type="button" className="btn danger" onClick={() => setPopup("supprimer")}>Supprimer la salle</button>
                </div>
            )}

            {info && (
                <p className="note-chg" role="status">
                    {info}
                    <button type="button" className="muted small lien" onClick={() => setInfo("")}> Fermer</button>
                </p>
            )}

            <section className="plan">
                <div className="plan-corps ouvert">
                    <div>
                        <div className="plan-salle">
                            {/* haut de la salle : matériel de salle à gauche, poste formateur à droite */}
                            <div className="plan-haut">
                                <div className="plan-materiels">
                                    {peutGerer
                                        ? materielsSalle.map(m => (
                                            <button key={m.id} type="button"
                                                    className={`plan-mat ${CLASSE_ETAT_MATERIEL[m.etat]}${m.id === materielId ? " select" : ""}`}
                                                    aria-pressed={m.id === materielId}
                                                    onClick={() => choisirMateriel(m.id)}>
                                                <b>{COURT[m.type]}</b>
                                                <span>{LIBELLE_ETAT_MATERIEL[m.etat]}</span>
                                            </button>
                                        ))
                                        // formateur : pas d'accès au détail du matériel, seulement le type
                                        : Object.entries(salle.materielsParType ?? {}).flatMap(([type, n]) =>
                                            Array.from({length: n ?? 0}, (_, i) => (
                                                <div key={`${type}-${i}`} className="plan-mat use">
                                                    <b>{COURT[type as TypeMateriel]}</b>
                                                </div>
                                            )))}
                                </div>
                                {posteFormateur && (
                                    <button type="button"
                                            className={`plan-poste formateur ${classeEtat(posteFormateur.etat)}${estompe(posteFormateur) ? " estompe" : ""}${posteFormateur.id === posteId ? " select" : ""}`}
                                            title="Poste formateur"
                                            aria-pressed={posteFormateur.id === posteId}
                                            onClick={() => choisirPoste(posteFormateur.id)}>
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
                                                        title={p.occupant ? `${nomPoste(p)} : ${p.occupant}` : nomPoste(p)}
                                                        aria-pressed={p.id === posteId}
                                                        onClick={() => choisirPoste(p.id)}>
                                                    <b>{numeroPoste(p)}</b>
                                                    <span>{p.occupant ? nomCourt(p.occupant) : libreOuEtat(p)}</span>
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
                            {ETATS_POSTE.map(({etat, libelle, classe}) => (
                                <li key={etat} className={classe}><i
                                    className={`sw ${classe}`}/>{libelle} ({compte(etat)})</li>
                            ))}
                        </ul>
                    </div>
                    {panneau}
                </div>
            </section>

            {/* liste blanche / noire qui s'applique dans la salle ; un formateur de la salle peut la surcharger */}
            {salleDeCours && <ProcessusSalle salle={salle}/>}

            {popup === "formateur" &&
                <AffecterFormateurPopup salle={salle} onClose={fermerPopup} onFait={apresAction}/>}
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
                                recharger();
                            })
                            .catch(() => setErreurFormateur("Impossible de retirer le formateur."));
                    }}>
                    {erreurFormateur && <p className="error-message" role="alert">{erreurFormateur}</p>}
                </Popup>
            )}
            {popup === "stage" && <AffecterStagePopup salle={salle} onClose={fermerPopup} onFait={apresAction}/>}
            {popup === "retirerStage" && salle.stageId && (
                <Popup
                    open
                    onClose={fermerPopup}
                    title={`Retirer le stage ${salle.stageNom}`}
                    description="Les postes de la salle redeviennent libres : plus aucun stagiaire n'y est installé."
                    confirmLabel="Retirer"
                    variant="danger"
                    onConfirm={() => {
                        setErreurStage("");
                        retirerStageDeSalle(salle.stageId!, salle.id)
                            .then(apresAction)
                            .catch(() => setErreurStage("Impossible de retirer le stage."));
                    }}>
                    {erreurStage && <p className="error-message" role="alert">{erreurStage}</p>}
                </Popup>
            )}
            {popup === "modifier" && (
                <ModifierSallePopup
                    salle={salle}
                    postes={postes}
                    onClose={fermerPopup}
                    onFait={manquants => {
                        const liste = Object.entries(manquants)
                            .map(([type, n]) => NOM_TYPE[type as TypeMateriel]
                                ? pluriel(n ?? 0, ...NOM_TYPE[type as TypeMateriel])
                                : `${n} ${type}`);
                        setInfo(liste.length
                            ? `Salle modifiée, mais il manque dans le stock : ${liste.join(", ")}. Les postes concernés sont à compléter.`
                            : "");
                        apresAction();
                    }}/>
            )}
            {popup === "master" && <PlaquerMasterSallePopup salle={salle} onClose={fermerPopup} onFait={apresAction}/>}
            {popup === "masterPoste" && posteSelectionne && (
                <PlaquerMasterSallePopup salle={salle} poste={posteSelectionne} onClose={fermerPopup} onFait={apresAction}/>
            )}
            {popup === "geler" && <GelerSallePopup salle={salle} onClose={fermerPopup} onFait={apresAction}/>}
            {popup === "deplacer" && (
                <DeplacerSallePopup salle={salle} postes={postes} onClose={fermerPopup}
                                    onFait={idCible => navigate(`/salles/${idCible}`)}/>
            )}
            {popup === "supprimer" && (
                <Popup
                    open
                    onClose={fermerPopup}
                    title={`Supprimer la salle ${salle.numSalle}`}
                    description={`Ses postes sont supprimés et tout son matériel retourne au stock du bâtiment${salle.stageNom ? `, le stage ${salle.stageNom} n'a plus de salle` : ""}. Cette action est définitive.`}
                    confirmLabel="Supprimer la salle"
                    variant="danger"
                    onConfirm={() => confirmer(() => supprimerSalle(salle.id), () => navigate(-1))}>
                    {erreurAction && <p className="error-message" role="alert">{erreurAction}</p>}
                </Popup>
            )}
            {popup === "convertir" && (
                <Popup
                    open
                    onClose={fermerPopup}
                    title={`Convertir la salle ${salle.numSalle} en salle de stockage`}
                    description={`La salle doit être vide : ni stage, ni stagiaire, ni formateur attribué.${salle.masterNom ? ` Le master ${salle.masterNom} est retiré.` : ""} Ses postes sont supprimés et tout leur matériel devient le stock de la salle.`}
                    confirmLabel="Convertir"
                    variant="danger"
                    onConfirm={() => confirmer(() => convertirEnStockage(salle.id), () => navigate(-1))}>
                    {erreurAction && <p className="error-message" role="alert">{erreurAction}</p>}
                </Popup>
            )}
            {popup === "retirerStagiaire" && posteSelectionne?.occupantId && (
                <Popup
                    open
                    onClose={fermerPopup}
                    title={`Retirer ${posteSelectionne.occupant} du poste`}
                    description={`${nomPoste(posteSelectionne)} redevient libre. Le stagiaire reste dans son stage et pourra être assis ailleurs.`}
                    confirmLabel="Retirer du poste"
                    variant="danger"
                    onConfirm={() => confirmer(() => retirerStagiaireDuPoste(posteSelectionne.id, posteSelectionne.occupantId!))}>
                    {erreurAction && <p className="error-message" role="alert">{erreurAction}</p>}
                </Popup>
            )}
            {popup === "stagiaire" && posteSelectionne && (
                <AffecterStagiairePopup
                    posteId={posteSelectionne.id}
                    numPoste={numeroPoste(posteSelectionne)}
                    stageId={salle.stageId}
                    onClose={fermerPopup}
                    onFait={apresAction}/>
            )}
            {popup === "materiel" && posteSelectionne && (
                <AffecterMaterielPopup
                    posteId={posteSelectionne.id}
                    titrePoste={nomPoste(posteSelectionne)}
                    numBatiment={salle.numBatiment}
                    materielsParType={posteSelectionne.materielsParType ?? {}}
                    onClose={fermerPopup}
                    onFait={apresAction}/>
            )}
        </>
    );
}