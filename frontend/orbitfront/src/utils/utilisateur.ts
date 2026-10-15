/**

 * Traduit la recherche de la page Utilisateurs en filtre RSQL pour le back

 * (même principe que versRsql dans utils/materiels.ts).

 * ";" = ET, "," = OU. Chaque mot tapé doit se trouver dans le nom, le prénom OU l'identifiant.

 * Ex. rechercheVersRsql("dur so") -> (nom=ilike="dur",prenom=ilike="dur",login=ilike="dur");(nom=ilike="so",...)

 */

export function rechercheVersRsql(recherche: string): string {

    const conditions: string[] = [];



    for (const mot of recherche.trim().split(/\s+/)) {

        const valeur = mot.replace(/["\\]/g, ""); // un guillemet casserait la syntaxe RSQL

        if (valeur) conditions.push(`(nom=ilike="${valeur}",prenom=ilike="${valeur}",login=ilike="${valeur}")`);

    }



    return conditions.join(";");

}

