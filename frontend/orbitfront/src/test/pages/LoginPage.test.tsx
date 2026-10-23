import {describe, expect, it} from "vitest";
import {render, screen} from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import {MemoryRouter, Route, Routes} from "react-router-dom";
import {LoginPage} from "../../pages/LoginPage.tsx";
import {appel, reponse, simulerFetch, unUtilisateur} from "../fabriques.ts";

function rendre(chemin = "/connexion") {
    render(
        <MemoryRouter initialEntries={[chemin]}>
            <Routes>
                <Route path="/connexion" element={<LoginPage/>}/>
                <Route path="/batiments" element={<p>Accueil administrateur</p>}/>
                <Route path="/poste" element={<p>Accueil stagiaire</p>}/>
            </Routes>
        </MemoryRouter>,
    );
    return userEvent.setup();
}

const identifiant = () => screen.getByLabelText("Identifiant");
const motDePasse = () => screen.getByLabelText("Mot de passe");
const seConnecter = () => screen.getByRole("button", {name: "Se connecter"});

describe("LoginPage", () => {
    it("demande les deux champs sans appeler le serveur", async () => {
        const fetch = simulerFetch();
        const user = rendre();

        await user.type(identifiant(), "   ");
        await user.click(seConnecter());

        expect(screen.getByRole("alert")).toHaveTextContent("Saisissez votre identifiant et votre mot de passe.");
        expect(fetch).not.toHaveBeenCalled();
    });

    it("connecte l'utilisateur (identifiant sans espaces) et l'envoie sur la page de son rôle", async () => {
        const fetch = simulerFetch(reponse(200, {accessToken: "abc", utilisateur: unUtilisateur({role: "ADMINISTRATEUR"})}));
        const user = rendre();

        await user.type(identifiant(), "  jdupont ");
        await user.type(motDePasse(), "secret");
        await user.click(seConnecter());

        expect(await screen.findByText("Accueil administrateur")).toBeInTheDocument();
        expect(JSON.parse(appel(fetch).init.body as string)).toEqual({username: "jdupont", password: "secret"});
    });

    it.each([
        [401, "Identifiant ou mot de passe incorrect."],
        [423, "Compte verrouillé. Contactez l'administrateur."],
        [403, "Compte désactivé. Contactez l'administrateur."],
        [500, "Le service est indisponible. Réessayez plus tard."],
    ])("affiche le message du code %i et vide le mot de passe", async (status, message) => {
        simulerFetch(reponse(status));
        const user = rendre();

        await user.type(identifiant(), "jdupont");
        await user.type(motDePasse(), "faux");
        await user.click(seConnecter());

        expect(await screen.findByRole("alert")).toHaveTextContent(message);
        expect(motDePasse()).toHaveValue("");
        expect(identifiant()).toHaveValue("jdupont");
    });

    it("prévient quand le serveur est injoignable", async () => {
        simulerFetch(new TypeError("Failed to fetch"));
        const user = rendre();

        await user.type(identifiant(), "jdupont");
        await user.type(motDePasse(), "secret{Enter}");

        expect(await screen.findByRole("alert")).toHaveTextContent("Le service est indisponible.");
    });

    it("désactive le bouton pendant la connexion", async () => {
        let repondre!: (r: Response) => void;
        simulerFetch().mockReturnValueOnce(new Promise<Response>(r => { repondre = r; })); // réponse en attente
        const user = rendre();

        await user.type(identifiant(), "jdupont");
        await user.type(motDePasse(), "secret");
        await user.click(seConnecter());

        expect(screen.getByRole("button", {name: "Connexion…"})).toBeDisabled();

        repondre(reponse(200, {accessToken: "abc", utilisateur: unUtilisateur({role: "STAGIAIRE"})}));
        expect(await screen.findByText("Accueil stagiaire")).toBeInTheDocument();
    });

    it("explique pourquoi on revient ici quand la session a expiré", () => {
        rendre("/connexion?expiree");
        expect(screen.getByRole("status")).toHaveTextContent("Votre session a expiré : reconnectez-vous.");
    });
});
