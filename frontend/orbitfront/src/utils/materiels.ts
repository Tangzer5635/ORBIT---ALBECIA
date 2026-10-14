import type { EtatMateriel, Materiel, TypeMateriel } from "../types/types.ts";

export type Localisation = "PARTOUT" | "SALLE" | "STOCK" | "ARCHIVE";

export interface Filtres {
    recherche: string;
    type: TypeMateriel | null;    // null = tous les types
    etat: EtatMateriel | null;    // null = tous les états
    localisation: Localisation;
}

// ordre d'affichage des boutons
export const ORDRE_TYPES: TypeMateriel[] = ["UC", "Ecran", "VideoProjecteur", "TableauInteractif"];

export const ORDRE_ETATS: EtatMateriel[] = ["NORMAL", "A_REMPLACER", "DEFECTUEUX", "DISPONIBLE"];

// [singulier, pluriel] pour les phrases du type "3 unités centrales" pour la fonction pluriel
export const NOM_TYPE: Record<TypeMateriel, [string, string]> = {
    UC: ["unité centrale", "unités centrales"],
    Ecran: ["écran", "écrans"],
    VideoProjecteur: ["vidéoprojecteur", "vidéoprojecteurs"],
    Imprimante: ["imprimante", "imprimantes"],
    TableauInteractif: ["tableau interactif", "tableaux interactifs"],
};

export const LIBELLE_LOCALISATION: Record<Localisation, string> = {
    PARTOUT: "Partout",
    SALLE: "En salle",
    STOCK: "En stock",
    ARCHIVE: "Archivés",
};

// "Écran" -> "ecran" on ignore majuscule et accent le diacritic c'est le caractère speciaux
const normaliser = (s: string) => s.normalize("NFD").replace(/\p{Diacritic}/gu, "").toLowerCase();

export function localisationDe(m: Materiel): Exclude<Localisation, "PARTOUT"> {
    if (m.etat === "ARCHIVE") return "ARCHIVE";
    return m.typeSalle === "SalleDeCours" ? "SALLE" : "STOCK";
}

export function filtrerMateriels(materiels: Materiel[], f: Filtres, libelleType: (t: TypeMateriel) => string): Materiel[] {
    const mots = normaliser(f.recherche).split(/\s+/).filter(Boolean);

    return materiels.filter((m) => {
        const loc = localisationDe(m);
        // "Partout" = tout le materiel en service
        if (f.localisation === "PARTOUT" ? loc === "ARCHIVE" : loc !== f.localisation) return false;
        if (f.type && m.type !== f.type) return false;
        if (f.etat && m.etat !== f.etat) return false;

        if (mots.length === 0) return true;
        const texte = normaliser([m.id, libelleType(m.type), m.modele, m.numSerie, m.numSalle ?? ""].join(" "));
        return mots.every((mot) => texte.includes(mot)); // chaque mot doit être trouvé
    });
}

// nombre de matériels en stock pour un type donné
export function compterStock(materiels: Materiel[], type: TypeMateriel): number {
    return materiels.filter((m) => m.type === type && localisationDe(m) === "STOCK").length;
}

/**
 * Traduit la recherche et le type en filtre RSQL pour le back.
 * ";" = ET, "," = OU. Chaque mot tapé doit se trouver dans le modèle OU le n° de série.
 * Ex. versRsql("dell 70", "UC") -> type==UC;(modele=ilike="dell",numSerie=ilike="dell");(modele=ilike="70",numSerie=ilike="70")
 */
export function versRsql(recherche: string, type: TypeMateriel | null): string {
    const conditions: string[] = [];
    if (type) conditions.push(`type==${type}`);

    for (const mot of recherche.trim().split(/\s+/)) {
        const valeur = mot.replace(/["\\]/g, ""); // un guillemet casserait la syntaxe RSQL
        if (valeur) conditions.push(`(modele=ilike="${valeur}",numSerie=ilike="${valeur}")`);
    }

    return conditions.join(";");
}