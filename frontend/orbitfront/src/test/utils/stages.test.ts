import {afterEach, beforeEach, describe, expect, it, vi} from "vitest";
import {statutDe} from "../../utils/stages.ts";
import {unStage} from "../fabriques.ts";

describe("statutDe", () => {
    // statutDe lit la date du jour : on la fige au 15 octobre 2026 à midi
    beforeEach(() => {
        vi.useFakeTimers();
        vi.setSystemTime(new Date(2026, 9, 15, 12, 0, 0));
    });
    afterEach(() => vi.useRealTimers());

    it("un stage qui commence plus tard est à venir", () => {
        expect(statutDe(unStage({dateDebut: "2026-10-16", dateFin: "2026-12-01"}))).toBe("A_VENIR");
    });

    it("un stage fini avant aujourd'hui est terminé", () => {
        expect(statutDe(unStage({dateDebut: "2026-09-01", dateFin: "2026-10-14"}))).toBe("TERMINE");
    });

    it("un stage en cours inclut le premier et le dernier jour", () => {
        expect(statutDe(unStage({dateDebut: "2026-10-15", dateFin: "2026-12-01"}))).toBe("EN_COURS");
        expect(statutDe(unStage({dateDebut: "2026-09-01", dateFin: "2026-10-15"}))).toBe("EN_COURS");
    });
});
