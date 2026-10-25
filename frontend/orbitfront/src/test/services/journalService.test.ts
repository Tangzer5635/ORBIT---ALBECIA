import {beforeEach, describe, expect, it} from "vitest";
import {getJournal, getJournalPeriode, JournalError} from "../../services/journalService.ts";
import {appel, connecter, reponse, simulerFetch, unePage, uneLigneJournal} from "../fabriques.ts";

const params = (url: string) => Object.fromEntries(new URL(url, "http://localhost").searchParams);

describe("journalService", () => {
    beforeEach(() => connecter());

    it("getJournal envoie les filtres renseignés", async () => {
        const fetch = simulerFetch(reponse(200, unePage([])));

        await getJournal({filter: 'detail=ilike="x"', action: "CONNEXION", debut: "2026-10-01T00:00:00"}, 3);

        expect(params(appel(fetch).url)).toEqual({
            page: "3", size: "50", filter: 'detail=ilike="x"', action: "CONNEXION", debut: "2026-10-01T00:00:00",
        });
    });

    it("getJournal lève une JournalError", async () => {
        simulerFetch(reponse(403));

        const erreur = await getJournal({}).catch(e => e);

        expect(erreur).toBeInstanceOf(JournalError);
        expect(erreur.status).toBe(403);
    });

    it("getJournalPeriode lit toutes les pages, du plus ancien au plus récent, jusqu'à la dernière", async () => {
        const a = uneLigneJournal({id: 1}), b = uneLigneJournal({id: 2}), c = uneLigneJournal({id: 3});
        const fetch = simulerFetch(
            reponse(200, unePage([a, b], {page: 0, last: false})),
            reponse(200, unePage([c], {page: 1, last: true})),
        );

        expect(await getJournalPeriode("2026-10-01", "2026-10-31")).toEqual([a, b, c]);
        expect(fetch).toHaveBeenCalledTimes(2);
        expect(params(appel(fetch, 1).url)).toEqual({
            page: "1", size: "500", debut: "2026-10-01T00:00:00", fin: "2026-10-31T23:59:59.999", sort: "dateHeure,asc",
        });
    });
});
