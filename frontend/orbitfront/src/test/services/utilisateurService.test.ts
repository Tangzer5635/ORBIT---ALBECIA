import {beforeEach, describe, expect, it} from "vitest";
import {
    changerVerrou, getUtilisateurs, modifierUtilisateur, supprimerUtilisateur, UtilisateurError,
} from "../../services/utilisateurService.ts";
import {appel, connecter, reponse, simulerFetch, unePage, unUtilisateur} from "../fabriques.ts";
import type {UtilisateurRequest} from "../../types/types.ts";

const params = (url: string) => Object.fromEntries(new URL(url, "http://localhost").searchParams);

describe("utilisateurService", () => {
    beforeEach(() => connecter(unUtilisateur({role: "ADMINISTRATEUR"})));

    describe("getUtilisateurs", () => {
        it("envoie les filtres, y compris active=false", async () => {
            const fetch = simulerFetch(reponse(200, unePage([unUtilisateur()])));

            await getUtilisateurs({filter: 'nom=ilike="du"', role: "FORMATEUR", active: false, verrouille: null}, 1, 10);

            expect(params(appel(fetch).url)).toEqual({
                page: "1", size: "10", filter: 'nom=ilike="du"', filtreRole: "FORMATEUR", active: "false",
            });
        });

        it("n'envoie que la pagination sans filtre", async () => {
            const fetch = simulerFetch(reponse(200, unePage([])));

            await getUtilisateurs({});

            expect(params(appel(fetch).url)).toEqual({page: "0", size: "20"});
        });
    });

    describe("messages d'erreur du back", () => {
        it("réunit les messages de validation {champ: message}", async () => {
            simulerFetch(reponse(400, {nom: "Le nom est obligatoire.", prenom: "Le prénom est obligatoire."}));

            const erreur = await getUtilisateurs({}).catch(e => e);

            expect(erreur).toBeInstanceOf(UtilisateurError);
            expect(erreur.detail).toBe("Le nom est obligatoire. Le prénom est obligatoire.");
        });

        it("garde un message texte tel quel", async () => {
            simulerFetch(reponse(409, "Impossible de supprimer son propre compte"));

            await expect(supprimerUtilisateur(1)).rejects.toMatchObject({
                status: 409, detail: "Impossible de supprimer son propre compte",
            });
        });

        it("n'a pas de détail si le corps est vide", async () => {
            simulerFetch(reponse(404));
            await expect(supprimerUtilisateur(99)).rejects.toMatchObject({status: 404, detail: undefined});
        });

        it("statut 0 si le serveur est injoignable", async () => {
            simulerFetch(new TypeError("Failed to fetch"));
            await expect(getUtilisateurs({})).rejects.toMatchObject({status: 0});
        });
    });

    describe("modifierUtilisateur", () => {
        const ancien = unUtilisateur({id: 5, role: "FORMATEUR", active: true});
        const demande = (s: Partial<UtilisateurRequest> = {}): UtilisateurRequest =>
            ({nom: "Dupont", prenom: "Jean", login: "jdupont", role: "FORMATEUR", active: true, ...s});

        it("n'envoie que le PUT de l'identité si le rôle et l'état n'ont pas changé", async () => {
            const fetch = simulerFetch(reponse(200, ancien));

            await modifierUtilisateur(5, demande(), ancien);

            expect(fetch).toHaveBeenCalledTimes(1);
            expect(appel(fetch).url).toBe("/api/v1/personnels/5/");
            expect(appel(fetch).init.method).toBe("PUT");
            expect(JSON.parse(appel(fetch).init.body as string)).toEqual({nom: "Dupont", prenom: "Jean", login: "jdupont"});
        });

        it("change le rôle puis désactive le compte avec leurs propres routes", async () => {
            const final = unUtilisateur({id: 5, role: "GESTIONNAIRE", active: false});
            const fetch = simulerFetch(reponse(200, ancien), reponse(200, {...ancien, role: "GESTIONNAIRE"}), reponse(200, final));

            const resultat = await modifierUtilisateur(5, demande({role: "GESTIONNAIRE", active: false}), ancien);

            expect(resultat).toEqual(final);
            expect(fetch.mock.calls.map(([url, init]) => `${init?.method} ${url}`)).toEqual([
                "PUT /api/v1/personnels/5/",
                "PATCH /api/v1/personnels/5/role/",
                "PATCH /api/v1/personnels/5/desactiver/",
            ]);
            expect(JSON.parse(appel(fetch, 1).init.body as string)).toEqual({role: "GESTIONNAIRE"});
        });

        it("s'arrête à la première erreur", async () => {
            const fetch = simulerFetch(reponse(400, {nom: "Nom trop long"}));

            await expect(modifierUtilisateur(5, demande({role: "GESTIONNAIRE"}), ancien)).rejects.toMatchObject({status: 400});
            expect(fetch).toHaveBeenCalledTimes(1);
        });
    });

    it.each([
        [true, "verrouiller"],
        [false, "deverrouiller"],
    ])("changerVerrou(%s) appelle /%s/", async (verrouiller, route) => {
        const fetch = simulerFetch(reponse(200, unUtilisateur()));

        await changerVerrou(5, verrouiller);

        expect(appel(fetch).url).toBe(`/api/v1/personnels/5/${route}/`);
        expect(appel(fetch).init.method).toBe("PATCH");
    });
});
