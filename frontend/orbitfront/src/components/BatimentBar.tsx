import {useCallback, useEffect, useRef, useState} from "react";
import {useLocation, useNavigate, useSearchParams} from "react-router-dom";
import {getUtilisateur} from "../services/authService.ts";
import {useBatiments} from "../context/BatimentsContext.tsx";
import {niveau, pourcentageUtilisable} from "../utils/postes.ts";

function IconeBatiment() {
    return (
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinejoin="round"
             aria-hidden="true">
            <path d="M4 21V5l8-3 8 3v16"/>
            <path d="M2 21h20M9 21v-4h6v4M8 8h2M14 8h2M8 12h2M14 12h2"/>
        </svg>
    );
}

function Fleche({sens, ...props}: { sens: "gauche" | "droite" } & React.ButtonHTMLAttributes<HTMLButtonElement>) {
    return (
        <button type="button" className="batimentBar-fleche" {...props}>
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round"
                 strokeLinejoin="round" aria-hidden="true">
                <path d={sens === "gauche" ? "M15 6l-6 6 6 6" : "M9 6l6 6-6 6"}/>
            </svg>
        </button>
    );
}

export default function BatimentBar() {
    const {batiments} = useBatiments();
    const utilisateur = getUtilisateur();
    const navigate = useNavigate();
    const {pathname} = useLocation();
    const [params] = useSearchParams();

    const listeRef = useRef<HTMLElement>(null);
    const [peutGauche, setPeutGauche] = useState(false);
    const [peutDroite, setPeutDroite] = useState(false);
    // état du glisser à la souris
    const drag = useRef({enCours: false, aBouge: false, departX: 0, departScroll: 0});

    // le bâtiment sélectionné vient de l'URL : /salles?batiment=2
    const selection = pathname === "/salles" ? Number(params.get("batiment")) : null;

    // active/désactive les flèches selon la position de la liste
    const majFleches = useCallback(() => {
        const el = listeRef.current;
        if (!el) return;
        setPeutGauche(el.scrollLeft > 0);
        setPeutDroite(el.scrollLeft + el.clientWidth < el.scrollWidth - 1);
    }, []);

    // recalcul quand la liste change ou que la fenêtre est redimensionnée
    useEffect(() => {
        majFleches();
        window.addEventListener("resize", majFleches);
        return () => window.removeEventListener("resize", majFleches);
    }, [batiments, majFleches]);

    if (utilisateur?.role === "STAGIAIRE") return null;

    // glisse d'une largeur visible vers la gauche ou la droite
    function defiler(sens: -1 | 1) {
        const el = listeRef.current;
        if (el) el.scrollBy({left: sens * el.clientWidth, behavior: "smooth"});
    }

    // glisser à la souris
    function onPointerDown(e: React.PointerEvent<HTMLElement>) {
        if (e.pointerType !== "mouse" || e.button !== 0) return; // tactile : défilement natif
        drag.current = {enCours: true, aBouge: false, departX: e.clientX, departScroll: e.currentTarget.scrollLeft};
    }

    function onPointerMove(e: React.PointerEvent<HTMLElement>) {
        const d = drag.current;
        if (!d.enCours) return;
        const dx = e.clientX - d.departX;
        if (!d.aBouge && Math.abs(dx) > 5) {
            d.aBouge = true;
            e.currentTarget.classList.add("drag");
            e.currentTarget.setPointerCapture(e.pointerId);
        }
        if (d.aBouge) e.currentTarget.scrollLeft = d.departScroll - dx;
    }

    function onPointerFin(e: React.PointerEvent<HTMLElement>) {
        drag.current.enCours = false;
        e.currentTarget.classList.remove("drag");
        // le clic qui suit un glisser ne doit pas sélectionner un bâtiment
        setTimeout(() => { drag.current.aBouge = false; }, 0);
    }

    function onClickCapture(e: React.MouseEvent) {
        if (drag.current.aBouge) {
            e.preventDefault();
            e.stopPropagation();
        }
    }

    return (
        <div className="batimentBar">
            <div className="batimentBar-inner">
                <Fleche sens="gauche" aria-label="Bâtiments précédents"
                        disabled={!peutGauche} onClick={() => defiler(-1)}/>

                <nav className="batimentBar-liste" ref={listeRef} onScroll={majFleches} aria-label="Bâtiments"
                     onPointerDown={onPointerDown} onPointerMove={onPointerMove}
                     onPointerUp={onPointerFin} onPointerCancel={onPointerFin}
                     onClickCapture={onClickCapture}>
                    {batiments.map((b) => {
                        const pct = pourcentageUtilisable(b.anneau);
                        return (
                            <button key={b.id} type="button"
                                    className={"batiment" + (selection === b.id ? " active" : "")}
                                    onClick={() => navigate(`/salles?batiment=${b.id}`)}>
                                <IconeBatiment/>
                                {b.numBatiment}
                                {pct !== null && <span className={"pp " + niveau(pct)}>{pct}%</span>}
                            </button>
                        );
                    })}
                </nav>

                <Fleche sens="droite" aria-label="Bâtiments suivants"
                        disabled={!peutDroite} onClick={() => defiler(1)}/>
            </div>
        </div>
    );
}