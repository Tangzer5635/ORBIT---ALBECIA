import {useEffect, useState} from "react";
import {type CalendrierGaranties, LIBELLE_TYPE_MATERIEL, type Materiel} from "../types/types.ts";
import {getCalendrierGaranties, getMaterielsFinGarantie} from "../services/materielService.ts";
import {dateFr, pluriel} from "../utils/postes.ts";
import FicheGarantiePopup from "../components/Popup/Materiel/FicheGarantiePopup.tsx";

// "2027-03" = "Mars"
function nomMois(mois: string): string {
    const [annee, numero] = mois.split("-").map(Number);
    const nom = new Date(annee, numero - 1, 1).toLocaleDateString("fr-FR", {month: "long"});
    return nom.charAt(0).toUpperCase() + nom.slice(1);
}

// "2027-03" -> "2027-04-01" : 1er jour du mois suivant (borne exclue du filtre)
function moisSuivant(mois: string): string {
    const [annee, numero] = mois.split("-").map(Number);
    return numero === 12 ? `${annee + 1}-01-01` : `${annee}-${String(numero + 1).padStart(2, "0")}-01`;
}

const aujourdhuiIso = () => new Date().toLocaleDateString("sv-SE"); // "2026-10-19"

// couleur d'une ligne : rouge hors garantie, jaune sous 3 mois, vert sinon
function classeGarantie(fin: string): string {
    const aujourdhui = aujourdhuiIso();
    if (fin < aujourdhui) return "def";
    const dans90j = new Date();
    dans90j.setDate(dans90j.getDate() + 90);
    return fin <= dans90j.toLocaleDateString("sv-SE") ? "chg" : "use";
}

// ce qui est affiché sous le calendrier : un mois ("2027-03") ou le matériel déjà hors garantie
type Selection = string | "hors" | null;

export default function GarantiePage() {
    const aujourdhui = new Date();
    const anneeCourante = aujourdhui.getFullYear();
    const moisCourant = aujourdhui.getMonth();

    const [annee, setAnnee] = useState(anneeCourante);
    const [calendrier, setCalendrier] = useState<CalendrierGaranties>();
    const [erreur, setErreur] = useState("");
    const [version, setVersion] = useState(0); // +1 = recharger après une modification de garantie

    const [selection, setSelection] = useState<Selection>(null);
    // liste du mois choisi, mémorisée avec la sélection concernée
    const [liste, setListe] = useState<{ pour: Selection; materiels: Materiel[]; erreur?: string }>();
    const [fiche, setFiche] = useState<Materiel | null>(null);

    // recharge à chaque changement d'année
    useEffect(() => {
        let actif = true; // ignore une réponse arrivée après un nouveau clic sur les flèches
        getCalendrierGaranties(annee)
            .then((c) => {
                if (actif) {
                    setCalendrier(c);
                    setErreur("");
                }
            })
            .catch(() => {
                if (actif) setErreur("Impossible de charger le calendrier des garanties.");
            });
        return () => { actif = false; };
    }, [annee, version]);

    // matériels du mois choisi (ou déjà hors garantie)
    useEffect(() => {
        if (!selection) return;
        let actif = true;
        const appel = selection === "hors"
            ? getMaterielsFinGarantie(null, aujourdhuiIso())
            : getMaterielsFinGarantie(`${selection}-01`, moisSuivant(selection));
        appel
            .then((materiels) => { if (actif) setListe({pour: selection, materiels}); })
            .catch(() => { if (actif) setListe({pour: selection, materiels: [], erreur: "Impossible de charger le matériel."}); });
        return () => { actif = false; };
    }, [selection, version]);

    function changerAnnee(delta: number) {
        setAnnee((a) => a + delta);
        if (selection !== "hors") setSelection(null); // le mois choisi n'est plus affiché
    }

    // un 2e clic sur le même mois referme la liste
    const choisir = (s: Selection) => setSelection((actuelle) => (actuelle === s ? null : s));

    // tant que la réponse concerne une autre année, on est en chargement
    const chargement = calendrier?.annee !== annee;
    const mois = calendrier && !chargement ? calendrier.mois : [];
    const total = mois.reduce((somme, m) => somme + m.nb, 0);
    // le mois le plus chargé prend la couleur la plus foncée
    const max = Math.max(1, ...mois.map((m) => m.nb));

    const listeChargee = liste?.pour === selection ? liste : undefined;
    const titreListe = selection === "hors"
        ? "Matériel déjà hors garantie"
        : selection ? `Fins de garantie en ${nomMois(selection).toLowerCase()} ${selection.slice(0, 4)}` : "";

    return (
        <>
            <div className="sh">
                <h1>Calendrier des garanties {annee}</h1>
                <div className="yr">
                    <button type="button" onClick={() => changerAnnee(-1)} aria-label="Année précédente">‹</button>
                    <b>{annee}</b>
                    <button type="button" onClick={() => changerAnnee(1)} aria-label="Année suivante">›</button>
                </div>
            </div>

            <div className="cal-top">
                <span className="heatleg">Peu<i/><i/><i/><i/>Beaucoup de fins de garantie</span>
                {calendrier && calendrier.expirees > 0 && (
                    <button type="button" className="cal-hors" aria-pressed={selection === "hors"}
                            onClick={() => choisir("hors")}>
                        {pluriel(calendrier.expirees, "matériel")} déjà hors garantie
                    </button>
                )}
            </div>

            {erreur && <p className="error-message">{erreur}</p>}
            {chargement && !erreur && <p className="muted">Chargement du calendrier…</p>}

            {!chargement && (
                <>
                    <ol className="months">
                        {mois.map((m) => {
                            const numero = Number(m.mois.split("-")[1]) - 1;
                            const niveau = m.nb ? Math.min(4, Math.ceil(m.nb / max * 4)) : 0;
                            const passe = annee < anneeCourante || (annee === anneeCourante && numero < moisCourant);
                            const courant = annee === anneeCourante && numero === moisCourant;
                            return (
                                <li key={m.mois}>
                                    <button type="button"
                                            className={`month h${niveau}${passe ? " past" : ""}${courant ? " now" : ""}`}
                                            aria-pressed={selection === m.mois}
                                            disabled={m.nb === 0}
                                            onClick={() => choisir(m.mois)}>
                                        <span className="month-nom">{nomMois(m.mois)}{courant && <small>ce mois-ci</small>}</span>
                                        <span className={m.nb ? "ml" : "ml zero"}>
                                            <b>{m.nb}</b>{m.nb > 1 ? "fins" : "fin"} de garantie
                                        </span>
                                    </button>
                                </li>
                            );
                        })}
                    </ol>

                    <p className="cal-note">
                        {pluriel(total, "fin")} de garantie en {annee}. Une panne n'est plus prise en charge après la fin de garantie.
                        {" "}Cliquez sur un mois pour voir le détail.
                    </p>
                </>
            )}

            {selection && (
                <section className="cal-detail" aria-live="polite">
                    <div className="sh">
                        <h2>{titreListe}</h2>
                        <button type="button" className="btn sm" onClick={() => setSelection(null)}>Fermer</button>
                    </div>
                    {!listeChargee && <p className="muted">Chargement du matériel…</p>}
                    {listeChargee?.erreur && <p className="error-message" role="alert">{listeChargee.erreur}</p>}
                    {listeChargee && !listeChargee.erreur && listeChargee.materiels.length === 0 && (
                        <p className="muted">Aucun matériel.</p>
                    )}
                    {listeChargee && listeChargee.materiels.length > 0 && (
                        <div className="list">
                            {listeChargee.materiels.map((e) => {
                                const hors = e.dateFinGarantie < aujourdhuiIso();
                                return (
                                    <button key={e.id} type="button" className={`li ${classeGarantie(e.dateFinGarantie)}`}
                                            onClick={() => setFiche(e)}>
                                        <strong>{LIBELLE_TYPE_MATERIEL[e.type]} {e.modele}</strong>
                                        <strong>{hors ? "Terminée le" : "Fin le"} {dateFr(e.dateFinGarantie)}</strong>
                                        <span>Matériel n° {e.id}, n° de série <span className="mono">{e.numSerie}</span></span>
                                        <span>{e.typeSalle === "SalleStockage" ? `En stock, salle ${e.numSalle}` : e.numSalle ? `Salle ${e.numSalle}` : ""}</span>
                                    </button>
                                );
                            })}
                        </div>
                    )}
                </section>
            )}

            {fiche && (
                <FicheGarantiePopup materiel={fiche} onClose={() => setFiche(null)}
                                    onChange={() => setVersion((v) => v + 1)}/>
            )}
        </>
    );
}
