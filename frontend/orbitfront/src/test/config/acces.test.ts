import {describe, expect, it} from "vitest";
import {ACCES, PAGE_ACCUEIL} from "../../config/acces.ts";
import type {Role} from "../../types/types.ts";

const ROLES: Role[] = ["ADMINISTRATEUR", "GESTIONNAIRE", "FORMATEUR", "STAGIAIRE"];
const rolesDe = (path: string) => ACCES.find(a => a.path === path)?.roles;

describe("ACCES", () => {
    it("ne déclare pas deux fois la même route", () => {
        const paths = ACCES.map(a => a.path);
        expect(new Set(paths).size).toBe(paths.length);
    });

    it("chaque route est ouverte à au moins un rôle", () => {
        for (const acces of ACCES) expect(acces.roles.length, acces.path).toBeGreaterThan(0);
    });

    it("la page d'accueil de chaque rôle lui est bien accessible", () => {
        for (const role of ROLES) expect(rolesDe(PAGE_ACCUEIL[role]), role).toContain(role);
    });

    it("le détail d'une salle est protégé et caché du menu", () => {
        const detail = ACCES.find(a => a.path === "/salles/:salleId");
        expect(detail?.menu).toBe(false);
        expect(detail?.roles).not.toContain("STAGIAIRE");
    });

    it("les pages d'administration sont réservées à l'administrateur", () => {
        expect(rolesDe("/utilisateurs")).toEqual(["ADMINISTRATEUR"]);
        expect(rolesDe("/journal")).toEqual(["ADMINISTRATEUR"]);
    });
});
