import {useEffect, useState} from "react";
import {LIBELLE_ACTION, type Journalisation, type Page, type TypeAction} from "../types/types.ts";
import {getJournal, getJournalPeriode, JournalError} from "../services/journalService.ts";
import {auteurJournal, dateHeureJournal, journalVersCsv, rechercheJournalVersRsql} from "../utils/journal.ts";
import ExporterJournalPopup from "../components/Popup/Journal/ExporterJournalPopup.tsx";
import Pagination from "../components/Utils/Pagination.tsx";

const TAILLE_PAGE = 20;
const ACTIONS = Object.keys(LIBELLE_ACTION) as TypeAction[];

// Couleur du liseré : rouge = échec de connexion, orange = défaillance signalée, vert = résolue, gris = le reste
const COULEUR: Partial<Record<TypeAction, string>> = {
    ECHEC_CONNEXION: "def",
    SIGNALEMENT: "chg",
    RESOLUTION: "use",
};

// date du jour décalée de n jours, au format "2026-10-17" (format des <input type="date">)
function jour(decalage = 0): string {
    const d = new Date();
    d.setDate(d.getDate() + decalage);
    return d.toLocaleDateString("sv-SE");
}

type ExportPret = { du: string; au: string; nombre: number; csv: string };

// Journal des actions (administrateur uniquement) : export CSV sur une période, recherche, liste paginée
export default function JournalPage() {
    // liste
    const [resultat, setResultat] = useState<Page<Journalisation>>();
    const [erreurChargement, setErreurChargement] = useState<string>();
    const [saisie, setSaisie] = useState("");         // ce qui est tapé dans le champ
    const [recherche, setRecherche] = useState("");   // ce qui est envoyé au back
    const [filtreAction, setFiltreAction] = useState<TypeAction | null>(null);
    const [page, setPage] = useState(0);

    // export (par défaut : les 30 derniers jours, comme la maquette)
    const [du, setDu] = useState(jour(-30));
    const [au, setAu] = useState(jour());
    const [erreurExport, setErreurExport] = useState("");
    const [exportEnCours, setExportEnCours] = useState(false);
    const [exportPret, setExportPret] = useState<ExportPret | null>(null);

    // la recherche part 500 ms après la dernière frappe (même principe que UtilisateurPage)
    useEffect(() => {
        const minuteur = setTimeout(() => {
            setRecherche(saisie);
            setPage(0);
        }, 500);
        return () => clearTimeout(minuteur);
    }, [saisie]);

    useEffect(() => {
        let actif = true; // ignore une réponse qui arrive après un changement de recherche
        getJournal({filter: rechercheJournalVersRsql(recherche), action: filtreAction}, page, TAILLE_PAGE)
            .then((p) => {
                if (!actif) return;
                setResultat(p);
                setErreurChargement(undefined);
            })
            .catch((err) => {
                if (!actif) return;
                setErreurChargement(err instanceof JournalError && err.status === 403
                    ? "Seul l'administrateur peut consulter le journal."
                    : "Impossible de charger le journal.");
            });
        return () => { actif = false; };
    }, [recherche, filtreAction, page]);

    // un changement de filtre ramène à la première page
    function changerAction(a: TypeAction | null) {
        setFiltreAction(a);
        setPage(0);
    }

    async function exporter() {
        setErreurExport("");
        if (!du || !au) return setErreurExport("Choisissez les deux dates.");
        if (au < du) return setErreurExport("La date de fin doit être après la date de début.");

        setExportEnCours(true);
        try {
            const lignes = await getJournalPeriode(du, au);
            if (lignes.length === 0) setErreurExport("Aucune entrée sur cette période.");
            else setExportPret({du, au, nombre: lignes.length, csv: journalVersCsv(lignes)});
        } catch {
            setErreurExport("Le service est indisponible. Réessayez plus tard.");
        } finally {
            setExportEnCours(false);
        }
    }

    return (
        <>
            <div className="page-head">
                <h1>Journal</h1>
                {resultat && (
                    <span className="muted">{resultat.totalElements} entrée{resultat.totalElements > 1 ? "s" : ""}</span>
                )}
            </div>

            <section className="card-s" aria-labelledby="journal-export-titre">
                <h2 id="journal-export-titre">Exporter le journal</h2>
                <div className="defaillance-dates">
                    <label className="defaillance-date" htmlFor="journal-du">
                        <span>Du</span>
                        <input id="journal-du" type="date" value={du} max={au || undefined}
                               onChange={(e) => { setDu(e.target.value); setErreurExport(""); }}/>
                    </label>
                    <label className="defaillance-date" htmlFor="journal-au">
                        <span>Au</span>
                        <input id="journal-au" type="date" value={au} min={du || undefined}
                               onChange={(e) => { setAu(e.target.value); setErreurExport(""); }}/>
                    </label>
                    <button type="button" className="btn primary sm" disabled={exportEnCours} onClick={() => void exporter()}>
                        {exportEnCours ? "Export…" : "Exporter en CSV"}
                    </button>
                </div>
                {erreurExport && <p className="error-message" role="alert">{erreurExport}</p>}
            </section>

            <input
                className="search"
                type="search"
                placeholder="Rechercher une action ou un ID utilisateur"
                aria-label="Rechercher dans le journal"
                value={saisie}
                onChange={(e) => setSaisie(e.target.value)}
            />

            <div className="filters">
                <div className="seg" role="group" aria-label="Type d'action">
                    <button type="button" aria-pressed={filtreAction === null} onClick={() => changerAction(null)}>
                        Toutes les actions
                    </button>
                    {ACTIONS.map((a) => (
                        <button key={a} type="button" aria-pressed={filtreAction === a} onClick={() => changerAction(a)}>
                            {LIBELLE_ACTION[a]}
                        </button>
                    ))}
                </div>
            </div>

            {erreurChargement && <p className="error-message" role="alert">{erreurChargement}</p>}

            <div className="list">
                {resultat?.content.map((l) => {
                    const {date, heure} = dateHeureJournal(l.dateHeure);
                    return (
                        <div key={l.id} className={`li ${COULEUR[l.action] ?? "dispo"}`}>
                            <strong>{l.detail}</strong>
                            <strong>{LIBELLE_ACTION[l.action] ?? l.action}</strong>
                            <span>{auteurJournal(l)}, le {date} à {heure}</span>
                            <span/>
                        </div>
                    );
                })}
                {!resultat && !erreurChargement && <p className="muted">Chargement…</p>}
                {resultat && resultat.totalElements === 0 && <p className="muted">Aucune entrée ne correspond.</p>}
            </div>

            {/* boutons de changement de page (même principe que UtilisateurPage) */}
            {resultat && <Pagination page={resultat.page} totalPages={resultat.totalPages} onChange={setPage}
                                     info={`(${resultat.totalElements} logs)`}/>}

            {exportPret && <ExporterJournalPopup {...exportPret} onClose={() => setExportPret(null)}/>}
        </>
    );
}