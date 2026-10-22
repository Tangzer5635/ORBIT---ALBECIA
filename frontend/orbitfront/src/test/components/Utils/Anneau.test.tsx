import {describe, expect, it} from "vitest";
import {render, screen} from "@testing-library/react";
import Anneau from "../../../components/Utils/Anneau.tsx";
import {unAnneau} from "../../fabriques.ts";

describe("Anneau", () => {
    it("affiche un tiret quand il n'y a aucun poste", () => {
        render(<Anneau id="vide" comptes={unAnneau()}/>);

        expect(screen.getByRole("img", {name: "Aucun poste"})).toHaveTextContent("—");
    });

    it("affiche le pourcentage utilisable avec un arc par état présent", () => {
        const {container} = render(<Anneau id="b1" comptes={unAnneau({bonEtat: 2, defectueux: 1})}/>);

        expect(screen.getByRole("img", {name: "67 % des postes utilisables"})).toHaveTextContent("67%");
        expect(container.querySelectorAll("circle")).toHaveLength(3); // le fond + 2 arcs
    });

    it("le grand anneau précise « utilisable »", () => {
        render(<Anneau id="b2" comptes={unAnneau({libre: 1})} grand/>);

        expect(screen.getByRole("img")).toHaveTextContent("100%utilisable");
    });

    it("hachure les postes libres avec un motif propre à chaque anneau", () => {
        const {container} = render(<Anneau id="b3" comptes={unAnneau({libre: 1})}/>);

        expect(container.querySelector("pattern")).toHaveAttribute("id", "hachure-b3");
        expect(container.querySelectorAll("circle")[1].getAttribute("style")).toMatch(/stroke:\s*url\("?#hachure-b3"?\)/);
    });
});
