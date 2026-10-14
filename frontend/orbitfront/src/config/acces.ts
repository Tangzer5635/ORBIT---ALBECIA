import type { Role } from "../types/types.ts";

export type Acces = {
    path: string;
    label: string;
    roles: Role[];
    labelParRole?: Partial<Record<Role, string>>;
    // false : route protégée avec le role mais pas present dans la navbar
    menu?: boolean;
};

const ROLES_SALLES: Role[] = ["ADMINISTRATEUR", "GESTIONNAIRE", "FORMATEUR"];


export const ACCES: Acces[] = [
    { path: "/batiments",    label: "Bâtiments",    roles: ["ADMINISTRATEUR"] },
    { path: "/salles",       label: "Salles",       roles: ROLES_SALLES, labelParRole: { FORMATEUR: "Mes salles" } },
    { path: "/salles/:salleId", label: "Détail d'une salle", roles: ROLES_SALLES, menu: false },
    { path: "/defaillances", label: "Défaillances", roles: ["ADMINISTRATEUR", "GESTIONNAIRE", "FORMATEUR"] },
    { path: "/garanties",    label: "Garanties",    roles: ["ADMINISTRATEUR", "GESTIONNAIRE"] },
    { path: "/materiel",     label: "Matériel",     roles: ["ADMINISTRATEUR", "GESTIONNAIRE"] },
    { path: "/masters",      label: "Masters",      roles: ["ADMINISTRATEUR", "GESTIONNAIRE"] },
    { path: "/stages",       label: "Stages",       roles: ["ADMINISTRATEUR", "GESTIONNAIRE"] },
    { path: "/processus",    label: "Processus",    roles: ["ADMINISTRATEUR", "FORMATEUR"] },
    { path: "/poste",        label: "Mon poste",    roles: ["STAGIAIRE"] },
    { path: "/utilisateurs", label: "Utilisateurs", roles: ["ADMINISTRATEUR"] },
    { path: "/journal",      label: "Journal",      roles: ["ADMINISTRATEUR"] },
];

// Page d'accueil de chaque role après la connexion
export const PAGE_ACCUEIL: Record<Role, string> = {
    ADMINISTRATEUR: "/batiments",
    GESTIONNAIRE: "/salles",
    FORMATEUR: "/salles",
    STAGIAIRE: "/poste",
};