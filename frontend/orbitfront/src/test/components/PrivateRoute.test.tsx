import {describe, expect, it} from "vitest";
import {render, screen} from "@testing-library/react";
import {MemoryRouter, Route, Routes} from "react-router-dom";
import {PrivateRoute, RedirectionAccueil} from "../../components/PrivateRoute.tsx";
import {PAGE_ACCUEIL} from "../../config/acces.ts";
import {connecter, unUtilisateur} from "../fabriques.ts";
import type {Role} from "../../types/types.ts";

// Mini-application : une page protégée, la connexion et l'accueil de chaque rôle
function rendre(chemin: string, roles?: Role[]) {
    render(
        <MemoryRouter initialEntries={[chemin]}>
            <Routes>
                <Route path="/connexion" element={<p>Page de connexion</p>}/>
                <Route path="/" element={<RedirectionAccueil/>}/>
                <Route element={<PrivateRoute roles={roles}/>}>
                    <Route path="/protegee" element={<p>Page protégée</p>}/>
                </Route>
                {Object.entries(PAGE_ACCUEIL).map(([role, path]) => (
                    <Route key={role} path={path} element={<p>Accueil {role}</p>}/>
                ))}
            </Routes>
        </MemoryRouter>,
    );
}

describe("PrivateRoute", () => {
    it("renvoie à la connexion si personne n'est connecté", () => {
        rendre("/protegee");
        expect(screen.getByText("Page de connexion")).toBeInTheDocument();
    });

    it("affiche la page à tout utilisateur connecté si aucun rôle n'est exigé", () => {
        connecter(unUtilisateur({role: "STAGIAIRE"}));
        rendre("/protegee");
        expect(screen.getByText("Page protégée")).toBeInTheDocument();
    });

    it("affiche la page si le rôle est autorisé", () => {
        connecter(unUtilisateur({role: "GESTIONNAIRE"}));
        rendre("/protegee", ["ADMINISTRATEUR", "GESTIONNAIRE"]);
        expect(screen.getByText("Page protégée")).toBeInTheDocument();
    });

    it("renvoie vers l'accueil de son rôle si le rôle n'est pas autorisé", () => {
        connecter(unUtilisateur({role: "STAGIAIRE"}));
        rendre("/protegee", ["ADMINISTRATEUR"]);

        expect(screen.queryByText("Page protégée")).not.toBeInTheDocument();
        expect(screen.getByText("Accueil STAGIAIRE")).toBeInTheDocument();
    });
});

describe("RedirectionAccueil", () => {
    it("renvoie à la connexion si personne n'est connecté", () => {
        rendre("/");
        expect(screen.getByText("Page de connexion")).toBeInTheDocument();
    });

    it.each(Object.keys(PAGE_ACCUEIL) as Role[])("envoie un %s vers sa page d'accueil", (role) => {
        connecter(unUtilisateur({role}));
        rendre("/");
        expect(screen.getByText(`Accueil ${role}`)).toBeInTheDocument();
    });
});
