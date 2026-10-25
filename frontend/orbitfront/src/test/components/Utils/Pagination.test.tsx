import {describe, expect, it, vi} from "vitest";
import {render, screen} from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import Pagination from "../../../components/Utils/Pagination.tsx";

const bouton = (nom: string) => screen.getByRole("button", {name: nom});
const champ = () => screen.getByRole("spinbutton", {name: "Numéro de page"});

function rendre(props: Partial<Parameters<typeof Pagination>[0]> = {}) {
    const onChange = vi.fn();
    render(<Pagination page={4} totalPages={10} onChange={onChange} {...props}/>);
    return {onChange, user: userEvent.setup()};
}

describe("Pagination", () => {
    it("n'affiche rien s'il n'y a qu'une page", () => {
        const {container} = render(<Pagination page={0} totalPages={1} onChange={vi.fn()}/>);
        expect(container).toBeEmptyDOMElement();
    });

    it("affiche la page courante (à partir de 1), le total et l'info", () => {
        rendre({info: "(4458 matériels)"});

        expect(champ()).toHaveValue(5);
        expect(screen.getByRole("navigation", {name: "Pagination"})).toHaveTextContent("/ 10 (4458 matériels)");
    });

    it("désactive Premier et Précédent sur la première page", () => {
        rendre({page: 0});

        expect(bouton("Première page")).toBeDisabled();
        expect(bouton("Page précédente")).toBeDisabled();
        expect(bouton("Page suivante")).toBeEnabled();
        expect(bouton("Dernière page")).toBeEnabled();
    });

    it("désactive Suivant et Dernier sur la dernière page", () => {
        rendre({page: 9});

        expect(bouton("Page suivante")).toBeDisabled();
        expect(bouton("Dernière page")).toBeDisabled();
    });

    it("désactive tout pendant un chargement", () => {
        rendre({disabled: true});

        for (const b of screen.getAllByRole("button")) expect(b).toBeDisabled();
        expect(champ()).toBeDisabled();
    });

    it.each([
        ["Première page", 0],
        ["Page précédente", 3],
        ["Page suivante", 5],
        ["Dernière page", 9],
    ])("« %s » demande la page %i", async (nom, attendue) => {
        const {onChange, user} = rendre();

        await user.click(bouton(nom));

        expect(onChange).toHaveBeenCalledExactlyOnceWith(attendue);
    });

    it("va à la page tapée avec Entrée", async () => {
        const {onChange, user} = rendre();

        await user.clear(champ());
        await user.type(champ(), "7{Enter}");

        expect(onChange).toHaveBeenCalledExactlyOnceWith(6);
    });

    it("ramène un numéro trop grand à la dernière page", async () => {
        const {onChange, user} = rendre();

        await user.clear(champ());
        await user.type(champ(), "99{Enter}");

        expect(onChange).toHaveBeenCalledExactlyOnceWith(9);
        expect(champ()).toHaveValue(10);
    });

    it("remet le numéro courant si le champ est vidé, sans changer de page", async () => {
        const {onChange, user} = rendre();

        await user.clear(champ());
        await user.tab(); // quitte le champ

        expect(champ()).toHaveValue(5);
        expect(onChange).not.toHaveBeenCalled();
    });

    it("ne recharge pas si on retape la page courante", async () => {
        const {onChange, user} = rendre();

        await user.clear(champ());
        await user.type(champ(), "5{Enter}");

        expect(onChange).not.toHaveBeenCalled();
    });
});
