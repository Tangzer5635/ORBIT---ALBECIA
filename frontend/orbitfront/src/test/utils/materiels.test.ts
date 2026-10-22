import {describe, expect, it} from "vitest";
import {localisationDe, versRsql} from "../../utils/materiels.ts";
import {unMateriel} from "../fabriques.ts";

describe("versRsql", () => {
    it("renvoie un filtre vide quand rien n'est tapé", () => {
        expect(versRsql("")).toBe("");
        expect(versRsql("   ")).toBe("");
    });

    it("cherche un mot dans le modèle OU le numéro de série", () => {
        expect(versRsql("dell")).toBe('(modele=ilike="dell",numSerie=ilike="dell")');
    });

    it("exige chaque mot (ET) et ignore les espaces en trop", () => {
        expect(versRsql(" dell   70 ")).toBe('(modele=ilike="dell",numSerie=ilike="dell");(modele=ilike="70",numSerie=ilike="70")');
    });

    it("retire les guillemets et antislashs", () => {
        expect(versRsql('de"ll\\')).toBe('(modele=ilike="dell",numSerie=ilike="dell")');
        expect(versRsql('"')).toBe("");
    });
});

describe("localisationDe", () => {
    it("un matériel archivé est aux archives, même s'il a une salle", () => {
        expect(localisationDe(unMateriel({etat: "ARCHIVE", typeSalle: "SalleDeCours"}))).toBe("ARCHIVE");
    });

    it("un matériel dans une salle de cours est en salle", () => {
        expect(localisationDe(unMateriel({typeSalle: "SalleDeCours"}))).toBe("SALLE");
    });

    it("un matériel dans une salle de stockage est en stock", () => {
        expect(localisationDe(unMateriel({typeSalle: "SalleStockage"}))).toBe("STOCK");
    });

    it("sans salle connue, le matériel est considéré en stock", () => {
        expect(localisationDe(unMateriel({typeSalle: undefined}))).toBe("STOCK");
    });
});
