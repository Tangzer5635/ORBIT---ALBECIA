import type { Role } from "../types/types.ts";

export type Acces = {
    path: string;
    label: string;
    roles: Role[];
    labelParRole?: Partial<Record<Role, string>>;
};

export const ACCES: Acces[] = [
    { path: "/batiments",    label: "Bâtiments",    roles: ["ADMINISTRATEUR"] },
    { path: "/salles",       label: "Salles",       roles: ["ADMINISTRATEUR", "GESTIONNAIRE", "FORMATEUR"], labelParRole: { FORMATEUR: "Mes salles" } },
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