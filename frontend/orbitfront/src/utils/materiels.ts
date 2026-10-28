import type { EtatMateriel, Materiel, TypeMateriel } from "../types/types.ts";
import { pluriel } from "./postes.ts";

export type Localisation = "PARTOUT" | "SALLE" | "STOCK" | "ARCHIVE";

// ordre d'affichage des boutons
export const ORDRE_TYPES: TypeMateriel[] = ["UC", "Ecran", "VideoProjecteur", "TableauInteractif"];

export const ORDRE_ETATS: EtatMateriel[] = ["NORMAL", "A_REMPLACER", "DEFECTUEUX", "DISPONIBLE"];

// classe de couleur (index.css) de chaque état
export const CLASSE_ETAT_MATERIEL: Record<EtatMateriel, string> = {
    NORMAL: "use",
    A_REMPLACER: "chg",
    DEFECTUEUX: "def",
    DISPONIBLE: "dispo",
    ARCHIVE: "",
};

// [singulier, pluriel] pour les phrases du type "3 unités centrales" pour la fonction pluriel
export const NOM_TYPE: Record<TypeMateriel, [string, string]> = {
    UC: ["unité centrale", "unités centrales"],
    Ecran: ["écran", "écrans"],
    VideoProjecteur: ["vidéoprojecteur", "vidéoprojecteurs"],
    TableauInteractif: ["tableau interactif", "tableaux interactifs"],
};

// Composition d'un poste (règle du back) : 1 UC, 2 écrans au plus ; vidéoprojecteurs et tableaux = matériel de salle
export const MAX_PAR_POSTE: Record<TypeMateriel, number> = {
    UC: 1,
    Ecran: 2,
    VideoProjecteur: 0,
    TableauInteractif: 0,
};

export const estMaterielDeSalle = (type: TypeMateriel) => MAX_PAR_POSTE[type] === 0;

// le poste peut-il encore recevoir un matériel de ce type ?
export const placeSurPoste = (parType: Partial<Record<TypeMateriel, number>>, type: TypeMateriel) =>
    (parType[type] ?? 0) < MAX_PAR_POSTE[type];

export const LIBELLE_LOCALISATION: Record<Localisation, string> = {
    PARTOUT: "Partout",
    SALLE: "En salle",
    STOCK: "En stock",
    ARCHIVE: "Archivés",
};

export function localisationDe(m: Materiel): Exclude<Localisation, "PARTOUT"> {
    if (m.etat === "ARCHIVE") return "ARCHIVE";
    return m.typeSalle === "SalleDeCours" ? "SALLE" : "STOCK";
}

// ce qui manquait dans le stock à la création / modification d'une salle : "2 unités centrales, 1 écran"
export function texteManquants(manquants: Partial<Record<string, number>>): string {
    return Object.entries(manquants)
        .map(([type, n]) => type in NOM_TYPE
            ? pluriel(n ?? 0, ...NOM_TYPE[type as TypeMateriel])
            : `${n} ${type}`)
        .join(", ");
}

/**
 * Traduit la recherche et le type en filtre RSQL pour le back.
 * ";" = ET, "," = OU. Chaque mot tapé doit se trouver dans le modèle OU le n° de série.
 * Ex. versRsql("dell 70") -> (modele=ilike="dell",numSerie=ilike="dell");(modele=ilike="70",numSerie=ilike="70")
 */
export function versRsql(recherche: string): string {
    const conditions: string[] = [];

    for (const mot of recherche.trim().split(/\s+/)) {
        const valeur = mot.replace(/["\\]/g, ""); // un guillemet casserait la syntaxe RSQL
        if (valeur) conditions.push(`(modele=ilike="${valeur}",numSerie=ilike="${valeur}")`);
    }

    return conditions.join(";");
}