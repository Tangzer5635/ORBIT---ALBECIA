import {beforeEach, describe, expect, it, vi} from "vitest";
import {apiFetch, authHeaders} from "../../services/api.ts";
import {appel, connecter, reponse, simulerFetch, unUtilisateur} from "../fabriques.ts";

describe("authHeaders", () => {
    it("ajoute le token de la session", () => {
        connecter(unUtilisateur(), "abc");
        expect(authHeaders()).toEqual({"Content-Type": "application/json", Authorization: "Bearer abc"});
    });
});

describe("apiFetch", () => {
    beforeEach(() => connecter(unUtilisateur(), "ancien"));

    it("envoie le token et garde les options de l'appelant", async () => {
        const fetch = simulerFetch(reponse(200, {}));

        await apiFetch("/api/v1/materiels/1/", {method: "DELETE", headers: {"X-Test": "1"}});

        const {url, init, headers} = appel(fetch);
        expect(url).toBe("/api/v1/materiels/1/");
        expect(init.method).toBe("DELETE");
        expect(headers).toMatchObject({"X-Test": "1", Authorization: "Bearer ancien", "Content-Type": "application/json"});
    });

    it("laisse le navigateur choisir le Content-Type pour un fichier (FormData)", async () => {
        const fetch = simulerFetch(reponse(200, {}));

        await apiFetch("/api/v1/stages/import/", {method: "POST", body: new FormData()});

        expect(appel(fetch).headers).not.toHaveProperty("Content-Type");
        expect(appel(fetch).headers.Authorization).toBe("Bearer ancien");
    });

    it("renvoie telle quelle une réponse qui n'est pas un 401", async () => {
        const fetch = simulerFetch(reponse(500));

        const res = await apiFetch("/api/v1/salles/");

        expect(res.status).toBe(500);
        expect(fetch).toHaveBeenCalledTimes(1);
    });

    it("sur un 401, rafraîchit le token puis rejoue la requête avec le nouveau", async () => {
        const fetch = simulerFetch(
            reponse(401),
            reponse(200, {accessToken: "nouveau", utilisateur: unUtilisateur()}),
            reponse(200, {ok: true}),
        );

        const res = await apiFetch("/api/v1/salles/");

        expect(await res.json()).toEqual({ok: true});
        expect(appel(fetch, 1).url).toBe("/api/v1/auth/refresh/");
        expect(appel(fetch, 2).url).toBe("/api/v1/salles/");
        expect(appel(fetch, 2).headers.Authorization).toBe("Bearer nouveau");
    });

    it("si le refresh est refusé, termine la session et renvoie à la connexion", async () => {
        const assign = vi.fn();
        vi.stubGlobal("location", {...window.location, assign});
        simulerFetch(reponse(401), reponse(401));

        const res = await apiFetch("/api/v1/salles/");

        expect(res.status).toBe(401);
        expect(assign).toHaveBeenCalledWith("/connexion?expiree");
        expect(sessionStorage.getItem("orbit-token")).toBeNull();
        expect(sessionStorage.getItem("orbit-user")).toBeNull();
    });

    it("plusieurs 401 en même temps ne déclenchent qu'un seul refresh", async () => {
        const fetch = vi.fn<typeof globalThis.fetch>(async (url, init) => {
            if (String(url).endsWith("/auth/refresh/")) return reponse(200, {accessToken: "nouveau", utilisateur: unUtilisateur()});
            const token = (init?.headers as Record<string, string>).Authorization;
            return token === "Bearer nouveau" ? reponse(200, {}) : reponse(401);
        });
        vi.stubGlobal("fetch", fetch);

        const resultats = await Promise.all([apiFetch("/a"), apiFetch("/b"), apiFetch("/c")]);

        expect(resultats.map(r => r.status)).toEqual([200, 200, 200]);
        const refreshs = fetch.mock.calls.filter(([url]) => String(url).endsWith("/auth/refresh/"));
        expect(refreshs).toHaveLength(1);
    });
});
