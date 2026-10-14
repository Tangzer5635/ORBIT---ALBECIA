import type {Stage} from "../types/types.ts";

export type Statut = "EN_COURS" | "A_VENIR" | "TERMINE";

// Le statut se calcule avec les dates ("2026-10-14" se compare comme du texte)
export function statutDe(stage: Stage): Statut {
    const aujourdhui = new Date().toLocaleDateString("sv-SE");
    if (stage.dateDebut > aujourdhui) return "A_VENIR";
    if (stage.dateFin < aujourdhui) return "TERMINE";
    return "EN_COURS";
}