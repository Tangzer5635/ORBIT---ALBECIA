import { Outlet } from "react-router-dom";
import Navbar from "./Navbar.tsx";
import BatimentBar from "./BatimentBar.tsx";

// Barre de navigation + page courante
export function Layout() {
    return (
        <>
            <Navbar />
            <BatimentBar/>
            <main className="page">
                <Outlet />
            </main>
        </>
    );
}