// Chargé avant chaque fichier de test (voir "setupFiles" dans vite.config.ts)
import "@testing-library/jest-dom/vitest"; // ajoute toBeInTheDocument, toBeDisabled, toHaveValue...
import {cleanup} from "@testing-library/react";
import {afterEach, vi} from "vitest";

// Chaque test repart d'un état propre
afterEach(() => {
    cleanup();               // démonte les composants rendus
    sessionStorage.clear();  // token et utilisateur connecté
    vi.unstubAllGlobals();   // fetch, location... remis d'origine
    vi.restoreAllMocks();
    vi.useRealTimers();
});
