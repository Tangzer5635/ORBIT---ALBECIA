import { Outlet } from "react-router-dom";
import Navbar from "./Navbar.tsx";
import BatimentBar from "./Batiment/BatimentBar.tsx";
import { BatimentsProvider } from "../context/BatimentsContext.tsx";

// Barre de navigation + page courante
export function Layout() {
    return (
        <BatimentsProvider>
            <Navbar />
            <BatimentBar/>
            <main className="page">
                <Outlet />
            </main>
        </BatimentsProvider>
    );
}