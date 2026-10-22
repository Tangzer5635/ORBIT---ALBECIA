import {describe, expect, it, vi} from "vitest";
import {AuthError, finDeSession, getToken, getUtilisateur, login, logout, rafraichirToken} from "../../services/authService.ts";
import {appel, connecter, reponse, simulerFetch, unUtilisateur} from "../fabriques.ts";

describe("login", () => {
    it("envoie les identifiants et garde le token et l'utilisateur dans la session", async () => {
        const utilisateur = unUtilisateur({role: "ADMINISTRATEUR"});
        const fetch = simulerFetch(reponse(200, {accessToken: "abc", tokenType: "Bearer", utilisateur, expiresIn: 900}));

        const connecte = await login("jdupont", "secret");

        expect(connecte).toEqual(utilisateur);
        const {url, init} = appel(fetch);
        expect(url).toBe("/api/v1/auth/login/");
        expect(init.method).toBe("POST");
        expect(JSON.parse(init.body as string)).toEqual({username: "jdupont", password: "secret"});
        expect(getToken()).toBe("abc");
        expect(getUtilisateur()).toEqual(utilisateur);
    });

    it.each([401, 403, 423])("lève une AuthError %i sans rien garder", async (status) => {
        simulerFetch(reponse(status));

        const erreur = await login("jdupont", "faux").catch(e => e);

        expect(erreur).toBeInstanceOf(AuthError);
        expect(erreur.status).toBe(status);
        expect(getToken()).toBeNull();
    });

    it("lève une AuthError 0 si le serveur est injoignable", async () => {
        simulerFetch(new TypeError("Failed to fetch"));

        await expect(login("jdupont", "secret")).rejects.toMatchObject({status: 0});
    });
});

describe("logout", () => {
    it("envoie le token à révoquer et vide la session", async () => {
        connecter(unUtilisateur(), "abc");
        const fetch = simulerFetch(reponse(204));

        await logout();

        expect(appel(fetch).headers.Authorization).toBe("Bearer abc");
        expect(getToken()).toBeNull();
        expect(getUtilisateur()).toBeNull();
    });

    it("vide la session même si le serveur refuse", async () => {
        connecter();
        simulerFetch(reponse(500));

        await expect(logout()).rejects.toMatchObject({status: 500});
        expect(getToken()).toBeNull();
    });

    it("vide la session même si le serveur est injoignable", async () => {
        connecter();
        simulerFetch(new TypeError("Failed to fetch"));

        await expect(logout()).rejects.toBeInstanceOf(AuthError);
        expect(getUtilisateur()).toBeNull();
    });
});

describe("rafraichirToken", () => {
    it("remplace le token et met à jour l'utilisateur", async () => {
        connecter(unUtilisateur({verrouille: false}), "ancien");
        const aJour = unUtilisateur({verrouille: true});
        simulerFetch(reponse(200, {accessToken: "nouveau", utilisateur: aJour}));

        expect(await rafraichirToken()).toBe(true);
        expect(getToken()).toBe("nouveau");
        expect(getUtilisateur()).toEqual(aJour);
    });

    it("renvoie false si le refresh est refusé", async () => {
        simulerFetch(reponse(401));
        expect(await rafraichirToken()).toBe(false);
    });

    it("renvoie false si le serveur est injoignable", async () => {
        simulerFetch(new TypeError("Failed to fetch"));
        expect(await rafraichirToken()).toBe(false);
    });
});

describe("session", () => {
    it("getUtilisateur renvoie null quand personne n'est connecté", () => {
        expect(getUtilisateur()).toBeNull();
    });

    it("finDeSession vide la session et renvoie à la connexion", () => {
        const assign = vi.fn();
        vi.stubGlobal("location", {...window.location, assign});
        connecter();

        finDeSession();

        expect(getToken()).toBeNull();
        expect(assign).toHaveBeenCalledWith("/connexion?expiree");
    });
});
