import {describe, expect, it} from "vitest";
import {auteurJournal, dateHeureJournal, journalVersCsv, rechercheJournalVersRsql} from "../../utils/journal.ts";
import {uneLigneJournal} from "../fabriques.ts";
import type {Journalisation} from "../../types/types.ts";

describe("rechercheJournalVersRsql", () => {
    it("renvoie un filtre vide quand rien n'est tapé", () => {
        expect(rechercheJournalVersRsql("")).toBe("");
        expect(rechercheJournalVersRsql("   ")).toBe("");
    });

    it("cherche un mot dans le détail", () => {
        expect(rechercheJournalVersRsql("connexion")).toBe('detail=ilike="connexion"');
    });

    it("cherche aussi un nombre dans l'ID de l'auteur", () => {
        expect(rechercheJournalVersRsql("3")).toBe('(detail=ilike="3",auteurId==3)');
    });

    it("combine les mots avec un ET (;) et ignore les espaces en trop", () => {
        expect(rechercheJournalVersRsql("  connexion   3 ")).toBe('detail=ilike="connexion";(detail=ilike="3",auteurId==3)');
    });

    it("retire les guillemets et antislashs qui casseraient la syntaxe RSQL", () => {
        expect(rechercheJournalVersRsql('con"nex\\ion')).toBe('detail=ilike="connexion"');
        expect(rechercheJournalVersRsql('" \\')).toBe("");
    });
});

describe("dateHeureJournal", () => {
    it("découpe la date et l'heure sans décalage de fuseau", () => {
        expect(dateHeureJournal("2026-10-17T20:15:02.123456")).toEqual({date: "17/10/2026", heure: "20:15:02"});
    });

    it("tolère une date sans heure", () => {
        expect(dateHeureJournal("2026-01-05")).toEqual({date: "05/01/2026", heure: ""});
    });
});

describe("auteurJournal", () => {
    it("affiche l'ID de l'auteur", () => {
        expect(auteurJournal(uneLigneJournal({auteurId: 3}))).toBe("ID 3");
    });

    it("garde l'ID 0 (0 n'est pas une absence)", () => {
        expect(auteurJournal(uneLigneJournal({auteurId: 0}))).toBe("ID 0");
    });

    it("indique un auteur inconnu (ex. échec de connexion)", () => {
        expect(auteurJournal(uneLigneJournal({auteurId: undefined}))).toBe("Auteur inconnu");
    });
});

describe("journalVersCsv", () => {
    const ENTETE = "date;heure;id_utilisateur;action;detail";

    it("ne contient que l'en-tête si le journal est vide", () => {
        expect(journalVersCsv([])).toBe(ENTETE);
    });

    it("écrit une ligne par entrée avec le libellé de l'action", () => {
        const csv = journalVersCsv([
            uneLigneJournal(),
            uneLigneJournal({id: 2, dateHeure: "2026-10-18T08:00:00", auteurId: undefined, action: "ECHEC_CONNEXION", detail: "login inconnu"}),
        ]);
        expect(csv.split("\n")).toEqual([
            ENTETE,
            "17/10/2026;20:15:02;3;Connexion;OK",
            "18/10/2026;08:00:00;;Échec de connexion;login inconnu",
        ]);
    });

    it("met entre guillemets les cellules qui contiennent ; \" ou un retour à la ligne", () => {
        const csv = journalVersCsv([uneLigneJournal({detail: 'salle "B201";\nposte 3'})]);
        expect(csv.split("\n").slice(1).join("\n")).toBe('17/10/2026;20:15:02;3;Connexion;"salle ""B201"";\nposte 3"');
    });

    it("garde le code de l'action s'il n'a pas de libellé", () => {
        const ligne = uneLigneJournal({action: "NOUVELLE_ACTION" as Journalisation["action"]});
        expect(journalVersCsv([ligne])).toContain(";NOUVELLE_ACTION;");
    });
});
