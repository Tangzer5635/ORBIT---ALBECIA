import { useState } from "react";
import { getUtilisateur } from "../services/authService.ts";

const PAR_PAGE = 8;

const BATIMENTS = [
    { id: 1, nom: "Bâtiment 1" },
    { id: 2, nom: "Bâtiment 2" },
    { id: 3, nom: "Bâtiment 3" },
    { id: 4, nom: "Bâtiment 4" },
    { id: 5, nom: "Bâtiment 5" },
    { id: 6, nom: "Bâtiment 6" },
    { id: 7, nom: "Bâtiment 7" },
    { id: 8, nom: "Bâtiment 8" },
    { id: 9, nom: "Bâtiment 9" },
    { id: 10, nom: "Bâtiment 10" },
    { id: 11, nom: "Bâtiment 11" },
    { id: 12, nom: "Bâtiment 12" },
];

const MAX_DEBUT = Math.max(0, BATIMENTS.length - PAR_PAGE);

function IconeBatiment() {
    return (
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinejoin="round" aria-hidden="true">
            <path d="M4 21V5l8-3 8 3v16"/>
            <path d="M2 21h20M9 21v-4h6v4M8 8h2M14 8h2M8 12h2M14 12h2"/>
        </svg>
    );
}

function Fleche({ sens, ...props }: { sens: "gauche" | "droite" } & React.ButtonHTMLAttributes<HTMLButtonElement>) {
    return (
        <button type="button" className="batimentBar-fleche" {...props}>
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
                <path d={sens === "gauche" ? "M15 6l-6 6 6 6" : "M9 6l6 6-6 6"}/>
            </svg>
        </button>
    );
}

export default function BatimentBar() {
    const utilisateur = getUtilisateur();
    const [debut, setDebut] = useState(0);
    const [selection, setSelection] = useState(BATIMENTS[0].id);

    if (utilisateur?.role === "STAGIAIRE") return null;

    const visibles = BATIMENTS.slice(debut, debut + PAR_PAGE);
    const defilement = BATIMENTS.length > PAR_PAGE;

    return (
        <div className="batimentBar">
            <div className="batimentBar-inner">
                {defilement && (
                    <Fleche sens="gauche" aria-label="Bâtiments précédents" disabled={debut === 0} onClick={() => setDebut((d) => Math.max(0, d - PAR_PAGE))}/>
                )}

                <nav className="batimentBar-liste" aria-label="Bâtiments">
                    {visibles.map((b) => (
                        <button key={b.id} type="button"
                                className={"batiment" + (selection === b.id ? " active" : "")}
                                onClick={() => setSelection(b.id)}>
                            <IconeBatiment/>
                            {b.nom}
                        </button>
                    ))}
                </nav>

                {defilement && (
                    <Fleche sens="droite" aria-label="Bâtiments suivants"
                            disabled={debut >= MAX_DEBUT}
                            onClick={() => setDebut((d) => Math.min(MAX_DEBUT, d + PAR_PAGE))}/>
                )}
            </div>
        </div>
    );
}