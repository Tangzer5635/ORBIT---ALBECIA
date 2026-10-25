import {describe, expect, it, vi} from "vitest";
import {render, screen, waitFor} from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import CreerSallePopup from "../../../../components/Popup/Salle/CreerSallePopup.tsx";
import {appel, connecter, reponse, simulerFetch, unBatiment} from "../../../fabriques.ts";

function rendre() {
    connecter();
    const onClose = vi.fn();
    const onCree = vi.fn();
    render(<CreerSallePopup batiment={unBatiment({id: 1, numBatiment: "B201"})} onClose={onClose} onCree={onCree}/>);
    return {onClose, onCree, user: userEvent.setup()};
}

const bouton = (nom: string) => screen.getByRole("button", {name: nom});

// Étape 1 remplie avec la salle 204 au 2e étage, puis passage à l'étape 2
async function allerEtape2(user: ReturnType<typeof userEvent.setup>) {
    await user.type(screen.getByLabelText("Numéro de la salle"), "204");
    await user.selectOptions(screen.getByLabelText("Étage"), "2e étage");
    await user.click(bouton("Suivant"));
}

describe("CreerSallePopup", () => {
    it("exige le numéro de la salle", async () => {
        const {user} = rendre();

        await user.click(bouton("Suivant"));

        expect(screen.getByRole("alert")).toHaveTextContent("Le numéro de la salle est obligatoire.");
        expect(screen.getByLabelText("Numéro de la salle")).toBeInTheDocument(); // toujours à l'étape 1
    });

    it("crée une salle de cours équipée en 3 étapes", async () => {
        const fetch = simulerFetch(reponse(200, {idSalle: 9, manquants: {}}));
        const {user, onCree} = rendre();

        await allerEtape2(user);
        await user.clear(screen.getByLabelText("Postes stagiaires"));
        await user.type(screen.getByLabelText("Postes stagiaires"), "8");
        await user.click(bouton("2"));                                // écrans par poste
        await user.click(bouton("Ajouter : Tableau interactif"));
        await user.click(bouton("Suivant"));

        expect(screen.getByText("Bâtiment B201, salle 204, 2e étage :")).toBeInTheDocument();
        expect(screen.getByText(/Soit 9 unités centrales et 18 écrans/)).toBeInTheDocument();

        await user.click(bouton("Créer la salle"));

        await waitFor(() => expect(onCree).toHaveBeenCalledTimes(1));
        expect(appel(fetch).url).toBe("/api/v1/salles/equipee/");
        expect(JSON.parse(appel(fetch).init.body as string)).toEqual({
            idBatiment: 1, numSalle: "204", etage: "2", nbPostes: 8, ecransParPoste: 2, nbVideoProjecteurs: 1, nbTableaux: 1,
        });
    });

    // Échoue tant que le récapitulatif écrit le nombre deux fois (« 12 12 postes ») : pluriel() l'inclut déjà
    it("écrit chaque nombre une seule fois dans le récapitulatif", async () => {
        const {user} = rendre();

        await allerEtape2(user);
        await user.click(bouton("Suivant"));

        const recap = screen.getAllByRole("listitem").map(li => li.textContent);
        expect(recap).toContainEqual(expect.stringMatching(/^12 postes stagiaires? \+ 1 poste formateur$/));
        expect(recap).toContain("1 écran par poste");
        expect(recap).toContain("1 vidéo-projecteur");
    });

    it("garde la saisie en revenant à l'étape précédente", async () => {
        const {user, onClose} = rendre();

        await allerEtape2(user);
        await user.click(bouton("Précédent"));

        expect(screen.getByLabelText("Numéro de la salle")).toHaveValue("204");
        expect(onClose).not.toHaveBeenCalled();
    });

    it("ne descend pas sous 0 tableau interactif", async () => {
        const {user} = rendre();

        await allerEtape2(user);

        expect(bouton("Retirer : Tableau interactif")).toBeDisabled();
        expect(bouton("Retirer : Vidéoprojecteur")).toBeEnabled();
    });

    it("crée une salle de stockage vide en une seule étape", async () => {
        const fetch = simulerFetch(reponse(200, {id: 3}));
        const {user, onCree} = rendre();

        await user.click(bouton("Salle de stockage"));
        expect(screen.getByRole("heading", {name: "Créer une salle de stockage"})).toBeInTheDocument();
        await user.type(screen.getByLabelText("Numéro de la salle"), " S01 ");
        await user.click(bouton("Créer la salle"));

        await waitFor(() => expect(onCree).toHaveBeenCalledTimes(1));
        expect(appel(fetch).url).toBe("/api/v1/salles");
        expect(JSON.parse(appel(fetch).init.body as string)).toEqual({idBatiment: 1, numSalle: "S01", etage: "0", type: "SalleStockage"});
    });

    it.each([
        [reponse(400, "La salle S01 existe déjà dans ce bâtiment."), "La salle S01 existe déjà dans ce bâtiment."],
        [reponse(403), "Vous n'avez pas le droit de créer une salle."],
        [new TypeError("Failed to fetch"), "Le service est indisponible. Réessayez plus tard."],
    ])("affiche l'erreur du serveur (%#)", async (retour, message) => {
        simulerFetch(retour);
        const {user, onCree} = rendre();

        await user.click(bouton("Salle de stockage"));
        await user.type(screen.getByLabelText("Numéro de la salle"), "S01");
        await user.click(bouton("Créer la salle"));

        expect(await screen.findByRole("alert")).toHaveTextContent(message);
        expect(onCree).not.toHaveBeenCalled();
    });
});
