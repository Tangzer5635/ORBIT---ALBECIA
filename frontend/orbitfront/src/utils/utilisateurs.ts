import type { Role } from "../types/types.ts";

export type FiltreEtat = "" | "actifs" | "inactifs" | "verrouilles";

/**
 * Traduit les filtres de la page Utilisateurs en filtre RSQL pour le back.
 * ";" = ET, "," = OU. Chaque mot tapé doit se trouver dans le nom, le prénom OU l'identifiant.
 * Ex. versRsqlUtilisateurs("dup", "FORMATEUR", "actifs")
 *   -> role==FORMATEUR;active==true;(nom=ilike="dup",prenom=ilike="dup",login=ilike="dup")
 */
export function versRsqlUtilisateurs(recherche: string, role: Role | "", etat: FiltreEtat): string {
    const conditions: string[] = [];
    if (role) conditions.push(`role==${role}`);
    if (etat === "actifs") conditions.push("active==true");
    if (etat === "inactifs") conditions.push("active==false");
    if (etat === "verrouilles") conditions.push("verrouille==true");

    for (const mot of recherche.trim().split(/\s+/)) {
        const valeur = mot.replace(/["\\]/g, ""); // un guillemet casserait la syntaxe RSQL
        if (valeur) conditions.push(`(nom=ilike="${valeur}",prenom=ilike="${valeur}",login=ilike="${valeur}")`);
    }

    return conditions.join(";");
}