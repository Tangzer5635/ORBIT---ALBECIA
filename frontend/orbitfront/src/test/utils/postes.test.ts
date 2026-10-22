import {describe, expect, it} from "vitest";
import {
    batimentsParDisponibilite, dateFr, estFormateur, etatPoste, libelleEtage, niveau,
    nomCourt, nomPoste, numeroPoste, pluriel, pourcentageUtilisable, totalAnneau,
} from "../../utils/postes.ts";
import {unAnneau, unBatiment, unPoste} from "../fabriques.ts";

describe("totalAnneau", () => {
    it("vaut 0 sans anneau", () => {
        expect(totalAnneau(undefined)).toBe(0);
    });

    it("additionne tous les états", () => {
        expect(totalAnneau(unAnneau({libre: 1, bonEtat: 2, defectueux: 3, aChanger: 4}))).toBe(10);
    });
});

describe("pourcentageUtilisable", () => {
    it("vaut null sans anneau ou sans matériel", () => {
        expect(pourcentageUtilisable(undefined)).toBeNull();
        expect(pourcentageUtilisable(unAnneau())).toBeNull();
    });

    it("compte tout sauf le défectueux, arrondi", () => {
        expect(pourcentageUtilisable(unAnneau({bonEtat: 2, defectueux: 1}))).toBe(67);
        expect(pourcentageUtilisable(unAnneau({libre: 1, aChanger: 1}))).toBe(100);
        expect(pourcentageUtilisable(unAnneau({defectueux: 4}))).toBe(0);
    });
});

describe("libelleEtage", () => {
    it.each([
        ["0", "Rez-de-chaussée"],
        ["1", "1er étage"],
        ["2", "2e étage"],
        ["10", "10e étage"],
    ])("%s -> %s", (etage, libelle) => {
        expect(libelleEtage(etage)).toBe(libelle);
    });
});

describe("batimentsParDisponibilite", () => {
    const b = (numBatiment: string, bonEtat: number, defectueux: number) =>
        unBatiment({numBatiment, anneau: unAnneau({bonEtat, defectueux})});

    it("met les moins disponibles d'abord, les bâtiments sans poste à la fin, puis par numéro", () => {
        const tries = [b("B10", 0, 0), b("B3", 9, 1), b("B2", 1, 1), b("B1", 9, 1)].sort(batimentsParDisponibilite);
        expect(tries.map(x => x.numBatiment)).toEqual(["B2", "B1", "B3", "B10"]);
    });

    it("compare les numéros comme des nombres (B2 avant B10)", () => {
        const tries = [b("B10", 1, 0), b("B2", 1, 0)].sort(batimentsParDisponibilite);
        expect(tries.map(x => x.numBatiment)).toEqual(["B2", "B10"]);
    });
});

describe("niveau", () => {
    it.each([
        [0, "def"], [69, "def"],
        [70, "chg"], [89, "chg"],
        [90, "use"], [100, "use"],
    ] as const)("%i %% -> %s", (pct, attendu) => {
        expect(niveau(pct)).toBe(attendu);
    });
});

describe("pluriel", () => {
    it("accorde selon le nombre (0 et 1 au singulier)", () => {
        expect(pluriel(0, "salle")).toBe("0 salle");
        expect(pluriel(1, "salle")).toBe("1 salle");
        expect(pluriel(3, "salle")).toBe("3 salles");
    });

    it("accepte un pluriel irrégulier", () => {
        expect(pluriel(2, "unité centrale", "unités centrales")).toBe("2 unités centrales");
    });
});

describe("dateFr", () => {
    it("convertit une date ou une date-heure ISO sans décalage de fuseau", () => {
        expect(dateFr("2026-08-07")).toBe("07/08/2026");
        expect(dateFr("2026-08-07T23:59:00")).toBe("07/08/2026");
    });
});

describe("postes", () => {
    it("reconnaît le poste formateur", () => {
        expect(estFormateur(unPoste({numPoste: "B201-S302-F"}))).toBe(true);
        expect(estFormateur(unPoste({numPoste: "B201-S302-P07"}))).toBe(false);
    });

    it("extrait le numéro du poste (0 si absent)", () => {
        expect(numeroPoste(unPoste({numPoste: "B201-S302-P07"}))).toBe(7);
        expect(numeroPoste(unPoste({numPoste: "B201-S302-F"}))).toBe(0);
    });

    it("nomme le poste", () => {
        expect(nomPoste(unPoste({numPoste: "B201-S302-P7"}))).toBe("Poste 07");
        expect(nomPoste(unPoste({numPoste: "B201-S302-P12"}))).toBe("Poste 12");
        expect(nomPoste(unPoste({numPoste: "B201-S302-F"}))).toBe("Poste formateur");
    });

    it("retrouve la description d'un état de poste", () => {
        expect(etatPoste("A_COMPLETER")).toEqual({etat: "A_COMPLETER", libelle: "À compléter", classe: "inc"});
        expect(etatPoste(undefined)).toBeUndefined();
    });
});

describe("nomCourt", () => {
    it("abrège le nom de famille", () => {
        expect(nomCourt("Emma Bernard")).toBe("Emma B.");
    });

    it("garde le prénom seul s'il n'y a pas de nom", () => {
        expect(nomCourt("Emma")).toBe("Emma");
    });
});
