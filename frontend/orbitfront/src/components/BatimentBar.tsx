import {useEffect, useState} from "react";
import {getToken, getUtilisateur} from "../services/authService.ts";
import type {Batiment} from "../types/types.ts";


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
    const PAR_PAGE = 8;
    const [batiments, setBatiments] = useState<Batiment[]>([]);
    const MAX_DEBUT = Math.max(0, batiments.length - PAR_PAGE);
    const utilisateur = getUtilisateur();
    const [debut, setDebut] = useState(0);
    const [selection, setSelection] = useState<number | null>(null);

    useEffect(() => {
        fetch("http://localhost:8080/api/v1/batiments/", {
            headers: {Authorization: `Bearer ${getToken()}`},
        })
            .then(response => response.json())
            .then(data => setBatiments(data.content));
    }, [])

    if (utilisateur?.role === "STAGIAIRE") return null;

    const visibles = batiments.slice(debut, debut + PAR_PAGE);

    const defilement = batiments.length > PAR_PAGE;

    return (
        <div className="batimentBar">
            <div className="batimentBar-inner">
                {defilement && (
                    <Fleche sens="gauche" aria-label="Bâtiments précédents" disabled={debut === 0}
                            onClick={() => setDebut((d) => Math.max(0, d - PAR_PAGE))}/>
                )}

                <nav className="batimentBar-liste" aria-label="Bâtiments">
                    {visibles.map((b) => (
                        <button key={b.id} type="button"
                                className={"batiment" + (selection === b.id ? " active" : "")}
                                onClick={() => setSelection(b.id)}>
                            <IconeBatiment/>
                            {b.numBatiment}
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