import {useEffect, useRef, useState} from "react";
import {useLocation, useNavigate, useSearchParams} from "react-router-dom";
import {getUtilisateur} from "../../services/authService.ts";
import {useBatiments} from "../../context/BatimentsContext.tsx";
import {batimentsParDisponibilite, niveau, pourcentageUtilisable} from "../../utils/postes.ts";
import {getSalleById} from "../../services/salleService.ts";
import type {Batiment} from "../../types/types.ts";

function IconeBatiment() {
    return (
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinejoin="round"
             aria-hidden="true">
            <path d="M4 21V5l8-3 8 3v16"/>
            <path d="M2 21h20M9 21v-4h6v4M8 8h2M14 8h2M8 12h2M14 12h2"/>
        </svg>
    );
}

// pastille d'un bâtiment : icône, numéro et % de postes utilisables
function Pastille({batiment}: { batiment: Batiment }) {
    const pct = pourcentageUtilisable(batiment.anneau);
    return (
        <>
            <IconeBatiment/>
            {batiment.numBatiment}
            {pct !== null && <span className={"pp " + niveau(pct)}>{pct}%</span>}
        </>
    );
}

export default function BatimentBar() {
    const {batiments} = useBatiments();
    const utilisateur = getUtilisateur();
    const navigate = useNavigate();
    const {pathname} = useLocation();
    const [params] = useSearchParams();
    // "survol" : ouvert en passant sur le bouton, se ferme en quittant la barre
    // "clic" : ouvert au clic, reste ouvert jusqu'à un clic dehors, Échap ou un 2e clic
    const [mode, setMode] = useState<"survol" | "clic" | null>(null);
    const ouvert = mode !== null;
    const barRef = useRef<HTMLDivElement>(null);
    const role = utilisateur?.role;
    const cache = role === "STAGIAIRE" || role === "FORMATEUR";

    // page d'une salle (/salles/12) : on affiche son bâtiment, comme sur la liste des salles d'un bâtiment
    const idSalle = pathname.match(/^\/salles\/(\d+)/)?.[1] ?? null;
    const [salleBatiment, setSalleBatiment] = useState<{ idSalle: string; numBatiment: string | null } | null>(null);
    useEffect(() => {
        if (!idSalle || cache) return;
        let actif = true;
        getSalleById(idSalle)
            .then(s => { if (actif) setSalleBatiment({idSalle, numBatiment: s.numBatiment ?? null}); })
            .catch(() => { /* la barre reste sur « Bâtiments » */ });
        return () => { actif = false; };
    }, [idSalle, cache]);

    // menu ouvert : un clic en dehors de la barre ou Échap le ferme
    useEffect(() => {
        if (!ouvert) return;
        function clicDehors(e: PointerEvent) {
            if (!barRef.current?.contains(e.target as Node)) setMode(null);
        }
        function echap(e: KeyboardEvent) {
            if (e.key === "Escape") setMode(null);
        }
        document.addEventListener("pointerdown", clicDehors);
        document.addEventListener("keydown", echap);
        return () => {
            document.removeEventListener("pointerdown", clicDehors);
            document.removeEventListener("keydown", echap);
        };
    }, [ouvert]);

    if (cache) return null;

    // le bâtiment sélectionné vient de l'URL : /salles?batiment=2, ou de la salle affichée : /salles/12
    const numSalleBatiment = idSalle && salleBatiment?.idSalle === idSalle ? salleBatiment.numBatiment : null;
    const selection = pathname === "/salles" ? Number(params.get("batiment"))
        : batiments.find((b) => b.numBatiment === numSalleBatiment)?.id ?? null;
    const courant = batiments.find((b) => b.id === selection);
    const tries = [...batiments].sort(batimentsParDisponibilite);

    function choisir(id: number) {
        setMode(null);
        navigate(`/salles?batiment=${id}`);
    }

    return (
        <div className="batimentBar" ref={barRef}
             onMouseLeave={() => setMode((m) => (m === "survol" ? null : m))}>
            <div className="batimentBar-inner">
                <button type="button" className={"batiment" + (courant ? " active" : "")}
                        aria-expanded={ouvert} aria-controls="batimentBar-menu"
                        onMouseEnter={() => setMode((m) => m ?? "survol")}
                        onClick={() => setMode((m) => (m === "clic" ? null : "clic"))}>
                    {courant ? <Pastille batiment={courant}/> : <><IconeBatiment/>Bâtiments</>}
                    <svg className={"batimentBar-chevron" + (ouvert ? " ouvert" : "")} viewBox="0 0 24 24" fill="none"
                         stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"
                         aria-hidden="true">
                        <path d="M6 9l6 6 6-6"/>
                    </svg>
                </button>
                <span className="muted">{batiments.length} bâtiment{batiments.length > 1 ? "s" : ""}</span>
            </div>

            {ouvert && (
                <nav id="batimentBar-menu" className="batimentBar-menu" aria-label="Bâtiments">
                    <div className="batimentBar-grille">
                        {tries.map((b) => (
                            <button key={b.id} type="button"
                                    className={"batiment" + (selection === b.id ? " active" : "")}
                                    onClick={() => choisir(b.id)}>
                                <Pastille batiment={b}/>
                            </button>
                        ))}
                    </div>
                </nav>
            )}
        </div>
    );
}