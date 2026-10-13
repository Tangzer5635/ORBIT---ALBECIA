import type { Comptes } from "../types/types.ts";
import { ORDRE_ETATS as ORDRE, pourcentageUtilisable, totalPostes } from "../utils/postes.ts";

interface AnneauProps {
    // identifiant unique : sert à nommer la hachure SVG (plusieurs anneaux sur la même page)
    id: string | number;
    comptes: Comptes;
    // grand anneau (en-tête de page) avec "utilisables" sous le pourcentage
    grand?: boolean;
}

export default function Anneau({id, comptes, grand = false}: AnneauProps) {
    const epaisseur = grand ? 5 : 6;
    const total = totalPostes(comptes);
    const pct = pourcentageUtilisable(comptes);

    // un arc par état : longueur = part en %, décalage = parts déjà dessinées
    const segments: { k: keyof Comptes; part: number; decalage: number }[] = [];
    let cumul = 0;
    for (const k of ORDRE) {
        const part = total ? (comptes[k] / total) * 100 : 0;
        if (part) segments.push({ k, part, decalage: 25 - cumul });
        cumul += part;
    }

    return (
        <div className={grand ? "ring grand" : "ring"} role="img" aria-label={pct === null ? "Aucun poste" : `${pct} % de postes utilisables`}>
            <svg viewBox="0 0 42 42" aria-hidden="true">
                <defs>
                    <pattern id={`hachure-${id}`} patternUnits="userSpaceOnUse" width="1.8" height="1.8"
                             patternTransform="rotate(45)">
                        <rect width="1.8" height="1.8" style={{fill: "var(--use-bg)"}}/>
                        <rect width=".8" height="1.8" style={{fill: "var(--use)"}}/>
                    </pattern>
                </defs>
                <circle r="15.9155" cx="21" cy="21" fill="none" strokeWidth={epaisseur} style={{stroke: "var(--card2)"}}/>
                {segments.map(({ k, part, decalage }) => (
                    <circle key={k} r="15.9155" cx="21" cy="21" fill="none" strokeWidth={epaisseur}
                            style={{stroke: k === "dispo" ? `url(#hachure-${id})` : `var(--${k})`}}
                            strokeDasharray={`${part} ${100 - part}`} strokeDashoffset={decalage}/>
                ))}
            </svg>
            <div>
                <b>{pct === null ? "—" : `${pct}%`}</b>
                {grand && <span>utilisables</span>}
            </div>
        </div>
    );
}