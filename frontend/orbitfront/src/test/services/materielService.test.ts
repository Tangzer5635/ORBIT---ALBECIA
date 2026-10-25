import {beforeEach, describe, expect, it} from "vitest";
import {
    affecterMateriel, compterMateriels, getCalendrierGaranties, getMateriels, getMaterielsFinGarantie,
    MaterielError, remettreEnStock, supprimerMateriel,
} from "../../services/materielService.ts";
import {appel, connecter, reponse, simulerFetch, unePage, unMateriel, unUtilisateur} from "../fabriques.ts";

// paramètres de l'URL appelée
const params = (url: string) => new URL(url, "http://localhost").searchParams;

describe("materielService", () => {
    beforeEach(() => connecter());

    describe("getMateriels", () => {
        it("envoie la page, la taille, le tri et tous les filtres (encodés)", async () => {
            const page = unePage([unMateriel()]);
            const fetch = simulerFetch(reponse(200, page));
            const filter = '(modele=ilike="dell 7",numSerie=ilike="dell 7")';

            const resultat = await getMateriels({filter, type: "Ecran", etat: "DEFECTUEUX", localisation: "STOCK"}, 2, 50);

            expect(resultat).toEqual(page);
            const p = params(appel(fetch).url);
            expect(Object.fromEntries(p)).toEqual({
                page: "2", size: "50", sort: "id", filter, type: "Ecran", etat: "DEFECTUEUX", localisation: "STOCK",
            });
        });

        it("n'envoie pas les filtres vides", async () => {
            const fetch = simulerFetch(reponse(200, unePage([])));

            await getMateriels({filter: "", type: null, etat: null});

            expect([...params(appel(fetch).url).keys()]).toEqual(["page", "size", "sort"]);
        });

        it("lève une MaterielError avec le message du back", async () => {
            simulerFetch(reponse(400, "Filtre invalide"));

            const erreur = await getMateriels({}).catch(e => e);

            expect(erreur).toBeInstanceOf(MaterielError);
            expect(erreur).toMatchObject({status: 400, detail: "Filtre invalide"});
        });

        it("lève une MaterielError 0 si le serveur est injoignable", async () => {
            simulerFetch(new TypeError("Failed to fetch"));
            await expect(getMateriels({})).rejects.toMatchObject({status: 0});
        });
    });

    it("compterMateriels demande une page de 1 et lit totalElements", async () => {
        const fetch = simulerFetch(reponse(200, unePage([unMateriel()], {totalElements: 316, size: 1})));

        expect(await compterMateriels({type: "UC"})).toBe(316);
        expect(params(appel(fetch).url).get("size")).toBe("1");
    });

    it("supprimerMateriel accepte une réponse vide (204)", async () => {
        const fetch = simulerFetch(reponse(204));

        await expect(supprimerMateriel(7)).resolves.toBeUndefined();
        expect(appel(fetch).url).toBe("/api/v1/materiels/7/");
        expect(appel(fetch).init.method).toBe("DELETE");
    });

    it.each([
        [{salleId: 4}, "salleId=4"],
        [{posteId: 9}, "posteId=9"],
    ])("affecterMateriel vers %o", async (vers, attendu) => {
        const fetch = simulerFetch(reponse(200, unMateriel()));

        await affecterMateriel(3, vers);

        expect(appel(fetch).url).toBe(`/api/v1/materiels/3/?${attendu}`);
        expect(appel(fetch).init.method).toBe("POST");
    });

    it("remettreEnStock retire le matériel de son poste", async () => {
        const fetch = simulerFetch(reponse(200, unMateriel()));

        await remettreEnStock(3, {posteId: 9});

        expect(appel(fetch).url).toBe("/api/v1/materiels/3/?posteId=9");
        expect(appel(fetch).init.method).toBe("DELETE");
    });

    describe("getMaterielsFinGarantie", () => {
        it("filtre sur la période et le matériel en service", async () => {
            const materiels = [unMateriel()];
            const fetch = simulerFetch(reponse(200, unePage(materiels)));

            expect(await getMaterielsFinGarantie("2026-01-01", "2026-02-01")).toEqual(materiels);
            const p = params(appel(fetch).url);
            expect(p.get("filter")).toBe("dateFinGarantie=ge=2026-01-01;dateFinGarantie=lt=2026-02-01;archive==false");
            expect(p.get("sort")).toBe("dateFinGarantie,asc");
        });

        it("sans date de début, prend tout ce qui finit avant la date de fin", async () => {
            const fetch = simulerFetch(reponse(200, unePage([])));

            await getMaterielsFinGarantie(null, "2026-10-22");

            expect(params(appel(fetch).url).get("filter")).toBe("dateFinGarantie=lt=2026-10-22;archive==false");
        });
    });

    describe("getCalendrierGaranties", () => {
        it("demande l'année voulue", async () => {
            const calendrier = {annee: 2027, expirees: 3, mois: []};
            const fetch = simulerFetch(reponse(200, calendrier));

            expect(await getCalendrierGaranties(2027)).toEqual(calendrier);
            expect(appel(fetch).url).toBe("/api/v1/materiels/garanties/?annee=2027");
        });

        // Échoue tant que getCalendrierGaranties appelle fetch au lieu d'apiFetch (voir la revue)
        it("rafraîchit le token expiré comme les autres requêtes", async () => {
            const calendrier = {annee: 2027, expirees: 0, mois: []};
            simulerFetch(
                reponse(401),
                reponse(200, {accessToken: "nouveau", utilisateur: unUtilisateur()}),
                reponse(200, calendrier),
            );

            expect(await getCalendrierGaranties(2027)).toEqual(calendrier);
        });
    });
});
