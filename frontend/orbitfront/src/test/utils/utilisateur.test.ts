import {describe, expect, it} from "vitest";
import {rechercheVersRsql} from "../../utils/utilisateur.ts";

describe("rechercheVersRsql", () => {
    it("renvoie un filtre vide quand rien n'est tapé", () => {
        expect(rechercheVersRsql("  ")).toBe("");
    });

    it("cherche chaque mot dans le nom, le prénom OU l'identifiant", () => {
        expect(rechercheVersRsql("dur so")).toBe(
            '(nom=ilike="dur",prenom=ilike="dur",login=ilike="dur");' +
            '(nom=ilike="so",prenom=ilike="so",login=ilike="so")',
        );
    });

    it("retire les guillemets et antislashs", () => {
        expect(rechercheVersRsql('d"u\\r')).toBe('(nom=ilike="dur",prenom=ilike="dur",login=ilike="dur")');
    });
});
