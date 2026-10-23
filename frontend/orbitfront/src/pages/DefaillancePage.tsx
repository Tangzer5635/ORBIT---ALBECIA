import {useCallback, useEffect, useMemo, useState} from "react";
import {useSearchParams} from "react-router-dom";
import Popup from "../components/Popup/Popup.tsx";
import {getDefaillance, getDefaillances, getDefaillancesDuMateriel, resoudreDefaillance} from "../services/defaillanceService.ts";
import type {Defaillance, Page, StatutDefaillance} from "../types/types.ts";
import {LIBELLE_STATUT_DEFAILLANCE, LIBELLE_TYPE_MATERIEL} from "../types/types.ts";
import Pagination from "../components/Utils/Pagination.tsx";

const TAILLE_PAGE = 20;
const dateFr = (date?: string | null) => date
    ? new Date(date).toLocaleString("fr-FR", {dateStyle: "medium", timeStyle: "short"})
    : "—";

type Onglet = {statut: StatutDefaillance; label: string};
const ONGLETS: Onglet[] = [
    {statut: "SIGNALEE", label: "À traiter"},
    {statut: "RESOLUE", label: "Résolues"},
];

export default function DefaillancePage() {
    const [searchParams, setSearchParams] = useSearchParams();
    const [saisie, setSaisie] = useState("");
    const [recherche, setRecherche] = useState("");
    const [typeFiltre, setTypeFiltre] = useState("");
    const [dateDebut, setDateDebut] = useState("");
    const [dateFin, setDateFin] = useState("");
    const [resultatsRecherche, setResultatsRecherche] = useState<Defaillance[] | null>(null);
    const [statut, setStatut] = useState<StatutDefaillance>("SIGNALEE");
    const [page, setPage] = useState(0);
    const [resultat, setResultat] = useState<Page<Defaillance> | null>(null);
    const [compteurs, setCompteurs] = useState<Record<StatutDefaillance, number>>({SIGNALEE: 0, RESOLUE: 0});
    const [chargement, setChargement] = useState(true);
    const [erreur, setErreur] = useState("");
    const [selection, setSelection] = useState<Defaillance | null>(null);
    const [autres, setAutres] = useState<Defaillance[]>([]);
    const [noteSolution, setNoteSolution] = useState("");
    const [enregistrement, setEnregistrement] = useState(false);
    const [erreurAction, setErreurAction] = useState("");

    const charger = useCallback(async () => {
        setChargement(true);
        setErreur("");
        try {
            const [data, autre] = await Promise.all([
                getDefaillances(statut, page, TAILLE_PAGE),
                getDefaillances(statut === "SIGNALEE" ? "RESOLUE" : "SIGNALEE", 0, 1),
            ]);
            setResultat(data);
            setCompteurs({
                [statut]: data.totalElements,
                [statut === "SIGNALEE" ? "RESOLUE" : "SIGNALEE"]: autre.totalElements,
            } as Record<StatutDefaillance, number>);
        } catch (e) {
            setErreur(e instanceof Error ? e.message : "Impossible de charger les défaillances.");
        } finally {
            setChargement(false);
        }
    }, [statut, page]);

    useEffect(() => { void charger(); }, [charger]);

    // Comme dans Matériel, attend 500 ms après la dernière frappe avant de lancer la recherche.
    useEffect(() => {
        const minuteur = window.setTimeout(() => {
            setRecherche(saisie);
            setPage(0);
        }, 500);
        return () => window.clearTimeout(minuteur);
    }, [saisie]);

    // Ouvre la fiche ciblée même si elle n'est pas dans la page actuellement affichée.
    useEffect(() => {
        const id = Number(searchParams.get("ouvrir"));
        if (!Number.isInteger(id) || id <= 0) return;
        let actif = true;
        getDefaillance(id).then(d => {
            if (!actif) return;
            setStatut(d.statut);
            setSelection(d);
            setNoteSolution(d.noteSolution ?? "");
            setErreurAction("");
            getDefaillancesDuMateriel(d.idMateriel)
                .then(liste => { if (actif) setAutres(liste.filter(x => x.id !== d.id).sort((a, b) => b.dateSignalement.localeCompare(a.dateSignalement))); })
                .catch(() => { if (actif) setAutres([]); });
        }).catch(e => { if (actif) setErreur(e instanceof Error ? e.message : "Impossible d'ouvrir cette défaillance."); })
            .finally(() => { if (actif) setSearchParams(params => { params.delete("ouvrir"); return params; }, {replace: true}); });
        return () => { actif = false; };
    }, [searchParams, setSearchParams]);

    useEffect(() => {
        const q = recherche.trim();
        const filtresActifs = Boolean(q || typeFiltre || dateDebut || dateFin);
        if (!filtresActifs) { setResultatsRecherche(null); return; }
        let actif = true;
        setResultatsRecherche([]);
        void (async () => {
            try {
                const premiere = await getDefaillances(statut, 0, 100);
                const tout = [...premiere.content];
                for (let p = 1; p < premiere.totalPages; p++) {
                    const suivante = await getDefaillances(statut, p, 100);
                    tout.push(...suivante.content);
                }
                if (actif) setResultatsRecherche(tout);
            } catch {
                if (actif) setResultatsRecherche(resultat?.content ?? []);
            }
        })();
        return () => { actif = false; };
    }, [recherche, statut, typeFiltre, dateDebut, dateFin]);

    const lignesAffichees = useMemo(() => {
        const q = recherche.trim().toLocaleLowerCase("fr");
        const filtresActifs = Boolean(q || typeFiltre || dateDebut || dateFin);
        const base = filtresActifs ? (resultatsRecherche ?? []) : (resultat?.content ?? []);
        return base.filter(d => {
            const correspondRecherche = !q || [d.description, d.signalePar, d.traitePar ?? "", d.numSerieMateriel ?? "", String(d.id), LIBELLE_TYPE_MATERIEL[d.typeMateriel]]
                .some(v => v.toLocaleLowerCase("fr").includes(q));
            const jour = (d.statut === "RESOLUE" && d.dateResolution ? d.dateResolution : d.dateSignalement).slice(0, 10);
            return correspondRecherche
                && (!typeFiltre || d.typeMateriel === typeFiltre)
                && (!dateDebut || jour >= dateDebut)
                && (!dateFin || jour <= dateFin);
        });
    }, [resultat, recherche, resultatsRecherche, typeFiltre, dateDebut, dateFin]);

    const ouvrir = (d: Defaillance) => {
        setSelection(d);
        setNoteSolution(d.noteSolution ?? "");
        setErreurAction("");
        getDefaillancesDuMateriel(d.idMateriel)
            .then(liste => setAutres(liste.filter(x => x.id !== d.id).sort((a, b) => b.dateSignalement.localeCompare(a.dateSignalement))))
            .catch(() => setAutres([]));
    };

    const fermer = () => {
        if (enregistrement) return;
        setSelection(null);
        setErreurAction("");
    };

    const traiter = async () => {
        if (!selection || selection.statut !== "SIGNALEE") return;
        setEnregistrement(true);
        setErreurAction("");
        try {
            await resoudreDefaillance(selection.id, noteSolution);
            setSelection(null);
            await charger();
            // Actualise également le compteur de l'autre onglet sans changer l'onglet courant.
            const autreStatut: StatutDefaillance = statut === "SIGNALEE" ? "RESOLUE" : "SIGNALEE";
            try {
                const autre = await getDefaillances(autreStatut, 0, 1);
                setCompteurs(c => ({...c, [autreStatut]: autre.totalElements}));
            } catch { /* Le contenu principal reste affiché si seul le compteur échoue. */ }
        } catch (e) {
            setErreurAction(e instanceof Error ? e.message : "Impossible de résoudre cette défaillance.");
        } finally {
            setEnregistrement(false);
        }
    };

    return (
        <>
            <div className="sh">
                <div>
                    <h1>Défaillances</h1>
                    <p className="muted">Consultez les signalements, ouvrez leur fiche et consignez la résolution.</p>
                </div>
                <div className="seg" role="group" aria-label="Statut des défaillances">
                    {ONGLETS.map(o => (
                        <button key={o.statut} type="button" aria-pressed={statut === o.statut}
                                onClick={() => {setStatut(o.statut); setPage(0);}}>
                            {o.label} ({compteurs[o.statut]})
                        </button>
                    ))}
                </div>
            </div>

            <p className="muted small" style={{margin: "-4px 4px 14px"}}>
                {statut === "SIGNALEE"
                    ? "Les défaillances signalées sont à traiter. Ouvrez une ligne pour consulter les détails et la résoudre."
                    : "Historique des défaillances résolues, avec la personne qui les a traitées et la solution enregistrée."}
            </p>

            <input id="recherche-defaillance" className="search defaillance-search" type="search" value={saisie}
                   aria-label="Rechercher une défaillance"
                   placeholder="Rechercher une défaillance : description, n° de série, déclarant ou n° de fiche"
                   onChange={e => setSaisie(e.target.value)} />

            <div className="filters defaillance-filtres">
                <div className="seg defaillance-types" role="group" aria-label="Type de matériel">
                    <button type="button" aria-pressed={!typeFiltre}
                            onClick={() => { setTypeFiltre(""); setPage(0); }}>Tous les types</button>
                    {Object.entries(LIBELLE_TYPE_MATERIEL).map(([valeur, libelle]) =>
                        <button key={valeur} type="button" aria-pressed={typeFiltre === valeur}
                                onClick={() => { setTypeFiltre(typeFiltre === valeur ? "" : valeur); setPage(0); }}>
                            {libelle}
                        </button>)}
                </div>
                <div className="defaillance-dates" aria-label="Période">
                    <label className="defaillance-date" htmlFor="filtre-date-debut">
                        <span>Du</span>
                        <input id="filtre-date-debut" className="input" type="date" value={dateDebut}
                               onChange={e => { setDateDebut(e.target.value); setPage(0); }} />
                    </label>
                    <label className="defaillance-date" htmlFor="filtre-date-fin">
                        <span>Au</span>
                        <input id="filtre-date-fin" className="input" type="date" value={dateFin}
                               min={dateDebut || undefined}
                               onChange={e => { setDateFin(e.target.value); setPage(0); }} />
                    </label>
                    {(typeFiltre || dateDebut || dateFin || saisie) && <button type="button" className="btn sm defaillance-reset"
                                                                               onClick={() => { setSaisie(""); setRecherche(""); setTypeFiltre(""); setDateDebut(""); setDateFin(""); setPage(0); }}>Réinitialiser</button>}
                </div>
            </div>

            {erreur && <div className="error-message" role="alert">{erreur}
                <button type="button" className="btn sm" onClick={() => void charger()}>Réessayer</button>
            </div>}

            {(chargement || ((recherche.trim() || typeFiltre || dateDebut || dateFin) && resultatsRecherche === null)) ? <p className="muted">Recherche des défaillances…</p> : !erreur && (
                <div className="list">
                    {lignesAffichees.map(d => (
                        <button key={d.id} type="button" className={`li ${d.statut === "SIGNALEE" ? "def" : "use"}`}
                                style={{width: "100%", borderTop: 0, borderRight: 0, borderBottom: 0, textAlign: "left", cursor: "pointer"}}
                                onClick={() => ouvrir(d)}>
                            <strong>{d.description}</strong>
                            <strong>{LIBELLE_STATUT_DEFAILLANCE[d.statut]}</strong>
                            <span>{LIBELLE_TYPE_MATERIEL[d.typeMateriel]} · N° série {d.numSerieMateriel || "—"}</span>
                            <span>{d.statut === "RESOLUE"
                                ? `Résolue le ${dateFr(d.dateResolution)}${d.traitePar ? ` par ${d.traitePar}` : ""}`
                                : `${d.signalePar}, le ${dateFr(d.dateSignalement)}`}</span>
                        </button>
                    ))}
                    {lignesAffichees.length === 0 && <div className="empty">
                        <h2>{(recherche || typeFiltre || dateDebut || dateFin) ? "Aucun résultat." : statut === "SIGNALEE" ? "Aucune défaillance à traiter." : "Aucune défaillance résolue."}</h2>
                        <p className="muted">{(recherche || typeFiltre || dateDebut || dateFin) ? "Modifiez les filtres ou réinitialisez-les pour élargir la recherche." : "Les défaillances apparaîtront ici lorsqu'elles correspondent à cet onglet."}</p>
                    </div>}
                </div>
            )}

            {!recherche.trim() && !typeFiltre && !dateDebut && !dateFin && resultat && (
                <Pagination page={page} totalPages={resultat.totalPages} onChange={setPage} disabled={chargement}/>
            )}

            {/* titre vide : le titre est affiché dans l'en-tête coloré de la fiche */}
            <Popup open={selection !== null} onClose={fermer}
                   title=""
                   confirmLabel={selection?.statut === "SIGNALEE" ? "Résoudre" : "Fermer"}
                   onConfirm={() => selection?.statut === "SIGNALEE" ? void traiter() : fermer()}
                   loading={enregistrement} cancelLabel={selection?.statut === "SIGNALEE" ? "Annuler" : "Retour"}>
                {selection && <div className="defaillance-fiche">
                    {/* en-tête rouge (signalée) ou vert (résolue), comme la maquette */}
                    <div className={`f-head ${selection.statut === "SIGNALEE" ? "def" : "use"}`}>
                        <div>
                            <h2>{selection.description}</h2>
                            <p>Défaillance n° {selection.id}</p>
                        </div>
                        <span className="pastille">{LIBELLE_STATUT_DEFAILLANCE[selection.statut]}</span>
                    </div>
                    <section className="f-sec">
                        <h3>Signalement</h3>
                        <dl className="f-kv">
                            <dt>Remontée par</dt><dd>{selection.signalePar || "—"}</dd>
                            <dt>Le</dt><dd>{dateFr(selection.dateSignalement)}</dd>
                            <dt>Matériel</dt><dd>{LIBELLE_TYPE_MATERIEL[selection.typeMateriel]}</dd>
                            <dt>Numéro de série</dt><dd>{selection.numSerieMateriel || "—"}</dd>
                        </dl>
                    </section>
                    {autres.length > 0 && <section className="f-sec">
                        <h3>Autres défaillances sur ce matériel ({autres.length})</h3>
                        <ul className="f-pbs">
                            {autres.slice(0, 5).map(d => <li key={d.id} className={d.statut === "RESOLUE" ? "done" : "open"}>
                                <span><b>{d.description}</b><br/><small>{dateFr(d.dateSignalement)} · {LIBELLE_STATUT_DEFAILLANCE[d.statut]}</small></span>
                            </li>)}
                        </ul>
                    </section>}
                    {selection.statut === "RESOLUE" ? (
                        <section className="f-sec">
                            <h3>Résolution</h3>
                            <dl className="f-kv">
                                <dt>Résolue par</dt><dd>{selection.traitePar || "—"}</dd>
                                <dt>Le</dt><dd>{dateFr(selection.dateResolution)}</dd>
                                <dt>Solution</dt><dd>{selection.noteSolution || "Non précisée"}</dd>
                            </dl>
                        </section>
                    ) : (
                        <section className="f-sec">
                            <h3>Résoudre</h3>
                            <label className="lbl" htmlFor="note-solution">Ce qui a été fait (facultatif)</label>
                            <div className="quick" style={{display: "flex", flexWrap: "wrap", gap: 6, marginBottom: 8}}>
                                {["Matériel redémarré", "Câble remplacé", "Pièce changée", "Matériel remplacé depuis le stock"].map(s =>
                                    <button key={s} type="button" className="chip" onClick={() => setNoteSolution(s)}>{s}</button>)}
                            </div>
                            <textarea id="note-solution" className="input" rows={3} maxLength={500}
                                      placeholder="Ex. lampe du vidéoprojecteur changée"
                                      value={noteSolution} onChange={e => setNoteSolution(e.target.value)}/>
                            <p className="muted small">{noteSolution.length}/500 caractères</p>
                        </section>
                    )}
                    {erreurAction && <p className="error-message" role="alert">{erreurAction}</p>}
                </div>}
            </Popup>
        </>
    );
}