import {describe, expect, it, vi} from "vitest";
import {render, screen} from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import Popup from "../../../components/Popup/Popup.tsx";

function rendre(props: Partial<Parameters<typeof Popup>[0]> = {}) {
    const onClose = vi.fn();
    const onConfirm = vi.fn();
    const resultat = render(
        <Popup open title="Supprimer la salle" confirmLabel="Supprimer" onClose={onClose} onConfirm={onConfirm} {...props}>
            <label htmlFor="motif">Motif</label>
            <input id="motif"/>
        </Popup>,
    );
    return {...resultat, onClose, onConfirm, user: userEvent.setup()};
}

describe("Popup", () => {
    it("n'affiche rien quand elle est fermée", () => {
        rendre({open: false});
        expect(screen.queryByRole("dialog")).not.toBeInTheDocument();
    });

    it("affiche le titre, la description et le contenu", () => {
        rendre({description: "Cette action est définitive."});

        expect(screen.getByRole("dialog")).toBeInTheDocument();
        expect(screen.getByRole("heading", {name: "Supprimer la salle"})).toBeInTheDocument();
        expect(screen.getByText("Cette action est définitive.")).toBeInTheDocument();
        expect(screen.getByLabelText("Motif")).toBeInTheDocument();
    });

    it("confirme avec le bouton ou avec Entrée dans un champ", async () => {
        const {onConfirm, onClose, user} = rendre();

        await user.click(screen.getByRole("button", {name: "Supprimer"}));
        await user.type(screen.getByLabelText("Motif"), "travaux{Enter}");

        expect(onConfirm).toHaveBeenCalledTimes(2);
        expect(onClose).not.toHaveBeenCalled();
    });

    it.each([
        ["la touche Échap", (user: ReturnType<typeof userEvent.setup>) => user.keyboard("{Escape}")],
        ["la croix", (user: ReturnType<typeof userEvent.setup>) => user.click(screen.getByRole("button", {name: "Fermer"}))],
        ["Annuler", (user: ReturnType<typeof userEvent.setup>) => user.click(screen.getByRole("button", {name: "Annuler"}))],
    ])("se ferme avec %s", async (_, action) => {
        const {onClose, onConfirm, user} = rendre();

        await action(user);

        expect(onClose).toHaveBeenCalledTimes(1);
        expect(onConfirm).not.toHaveBeenCalled();
    });

    it("se ferme en cliquant à côté, mais pas en cliquant dedans", async () => {
        const {onClose, user} = rendre();
        const dialog = screen.getByRole("dialog");

        await user.click(screen.getByRole("heading"));
        expect(onClose).not.toHaveBeenCalled();

        await user.click(dialog.parentElement!); // le fond assombri
        expect(onClose).toHaveBeenCalledTimes(1);
    });

    it("utilise le bouton secondaire personnalisé (ex. « Précédent » d'un assistant)", async () => {
        const onCancel = vi.fn();
        const {onClose, user} = rendre({cancelLabel: "Précédent", onCancel});

        await user.click(screen.getByRole("button", {name: "Précédent"}));

        expect(onCancel).toHaveBeenCalledTimes(1);
        expect(onClose).not.toHaveBeenCalled();
    });

    it("bloque la confirmation pendant le chargement", () => {
        rendre({loading: true});

        const confirmer = screen.getByRole("button", {name: "…"});
        expect(confirmer).toBeDisabled();
        expect(screen.queryByRole("button", {name: "Supprimer"})).not.toBeInTheDocument();
    });

    it("bloque la confirmation quand disabled", () => {
        rendre({disabled: true});
        expect(screen.getByRole("button", {name: "Supprimer"})).toBeDisabled();
    });

    it("bloque le défilement de la page tant qu'elle est ouverte", () => {
        const {unmount} = rendre();
        expect(document.body.style.overflow).toBe("hidden");

        unmount();
        expect(document.body.style.overflow).toBe("");
    });
});
