import {LIBELLE_ACTION, type Journalisation} from "../types/types.ts";

/**
 * Traduit la recherche de la page Journal en filtre RSQL pour le back
 * (même principe que rechercheVersRsql dans utils/utilisateur.ts).
 * Chaque mot doit se trouver dans le détail ; un nombre peut aussi être l'ID de l'auteur.
 * Ex. rechercheJournalVersRsql("connexion 3") -> detail=ilike="connexion";(detail=ilike="3",auteurId==3)
 */
export function rechercheJournalVersRsql(recherche: string): string {
    const conditions: string[] = [];

    for (const mot of recherche.trim().split(/\s+/)) {
        const valeur = mot.replace(/["\\]/g, ""); // un guillemet casserait la syntaxe RSQL
        if (!valeur) continue;
        conditions.push(/^\d+$/.test(valeur)
            ? `(detail=ilike="${valeur}",auteurId==${valeur})`
            : `detail=ilike="${valeur}"`);
    }

    return conditions.join(";");
}

// "2026-10-17T20:15:02.123456" -> { date: "17/10/2026", heure: "20:15:02" }
// (découpage du texte : pas de new Date(), donc pas de décalage de fuseau)
export function dateHeureJournal(dateHeure: string): { date: string; heure: string } {
    const [date, heure = ""] = dateHeure.split("T");
    return {date: date.split("-").reverse().join("/"), heure: heure.slice(0, 8)};
}

// Qui a fait l'action : "ID 3", ou "Auteur inconnu" (ex. échec de connexion)
export function auteurJournal(l: Journalisation): string {
    return l.auteurId != null ? `ID ${l.auteurId}` : "Auteur inconnu";
}

// CSV séparé par « ; » (comme la maquette), une ligne par entrée du journal
export function journalVersCsv(lignes: Journalisation[]): string {
    const cellule = (v: string | number | undefined) => {
        const s = v == null ? "" : String(v);
        return /[;"\n]/.test(s) ? `"${s.replace(/"/g, '""')}"` : s;
    };
    return [
        "date;heure;id_utilisateur;action;detail",
        ...lignes.map(l => {
            const {date, heure} = dateHeureJournal(l.dateHeure);
            return [date, heure, l.auteurId, LIBELLE_ACTION[l.action] ?? l.action, l.detail].map(cellule).join(";");
        }),
    ].join("\n");
}