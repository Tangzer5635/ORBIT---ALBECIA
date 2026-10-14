import type { EtatMateriel, Materiel, TypeMateriel } from "../types/types.ts";

export type Localisation = "PARTOUT" | "SALLE" | "STOCK" | "ARCHIVE";

export interface Filtres {
    recherche: string;
    type: TypeMateriel | null;    // null = tous les types
    etat: EtatMateriel | null;    // null = tous les états
    localisation: Localisation;
}

// Ordre d'affichage des boutons (différent de l'ordre des clés dans types.ts)
export const ORDRE_TYPES: TypeMateriel[] = ["UC", "Ecran", "VideoProjecteur", "TableauInteractif"];
// ARCHIVE n'est pas un filtre d'état : c'est la localisation "Archivés"
export const ORDRE_ETATS: EtatMateriel[] = ["NORMAL", "A_REMPLACER", "DEFECTUEUX", "DISPONIBLE"];

export const LIBELLE_LOCALISATION: Record<Localisation, string> = {
    PARTOUT: "Partout",
    SALLE: "En salle",
    STOCK: "En stock",
    ARCHIVE: "Archivés",
};

// "Écran" -> "ecran" : la recherche ignore les majuscules et les accents
const normaliser = (s: string) => s.normalize("NFD").replace(/\p{Diacritic}/gu, "").toLowerCase();

export function localisationDe(m: Materiel): Exclude<Localisation, "PARTOUT"> {
    if (m.etat === "ARCHIVE") return "ARCHIVE";
    return m.typeSalle === "SalleDeCours" ? "SALLE" : "STOCK";
}

export function filtrerMateriels(materiels: Materiel[], f: Filtres, libelleType: (t: TypeMateriel) => string): Materiel[] {
    const mots = normaliser(f.recherche).split(/\s+/).filter(Boolean);

    return materiels.filter((m) => {
        const loc = localisationDe(m);
        // "Partout" = tout le matériel en service, les archivés ont leur propre filtre
        if (f.localisation === "PARTOUT" ? loc === "ARCHIVE" : loc !== f.localisation) return false;
        if (f.type && m.type !== f.type) return false;
        if (f.etat && m.etat !== f.etat) return false;

        if (mots.length === 0) return true;
        const texte = normaliser([m.id, libelleType(m.type), m.modele, m.numSerie, m.numSalle ?? ""].join(" "));
        return mots.every((mot) => texte.includes(mot)); // chaque mot doit être trouvé
    });
}