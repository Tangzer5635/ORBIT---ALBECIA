import { Outlet } from "react-router-dom";
import Navbar from "./Navbar.tsx";

// Barre de navigation + page courante
export function Layout() {
    return (
        <>
            <Navbar />
            <main className="page">
                <Outlet />
            </main>
        </>
    );
}