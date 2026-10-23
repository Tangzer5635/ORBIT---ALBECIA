import { useEffect, useState } from "react";
import { Link, NavLink, useLocation, useNavigate } from "react-router-dom";
import { getUtilisateur, logout } from "../services/authService.ts";
import logo from "../assets/orbit-logo.gif";
import { ACCES } from "../config/acces.ts";
import ThemeButton from "./Utils/ThemeButton.tsx";
import { EVT_DEFAILLANCES, getDefaillances } from "../services/defaillanceService.ts";

export default function Navbar() {
    const navigate = useNavigate();
    const { pathname } = useLocation();
    const utilisateur = getUtilisateur();
    const role = utilisateur?.role;
    const [aTraiter, setATraiter] = useState(0); // bulle à côté de « Défaillances »

    // défaillances à traiter (filtrées par le back : le formateur ne compte que celles de ses salles),
    // recomptées à chaque changement de page et après un signalement ou une résolution
    useEffect(() => {
        const accesDefaillances = ACCES.find((a) => a.path === "/defaillances")?.roles ?? [];
        if (!role || !accesDefaillances.includes(role)) return;
        const compter = () => {
            getDefaillances("SIGNALEE", 0, 1)
                .then((page) => setATraiter(page.totalElements))
                .catch(() => setATraiter(0));
        };
        compter();
        window.addEventListener(EVT_DEFAILLANCES, compter);
        return () => window.removeEventListener(EVT_DEFAILLANCES, compter);
    }, [role, pathname]);

    // si personne n'est connecté, pas de navbar (page de connexion)
    if (!utilisateur) return null;

    //on filtre en fonction du role et de l'apparition dans la navbar
    const liensVisibles = ACCES.filter((a) => a.menu !== false && a.roles.includes(utilisateur.role));

    async function handleLogout() {
        try {
            await logout();
        } catch {
            // s'il y a une erreur serveur la session locale est déjà vidée, on redirige quand même
        } finally {
            void navigate("/connexion", { replace: true });
        }
    }

    return (
        <header className="navbar">
            <div className="navbar-inner">
                <Link to="/accueil" className="navbar-brand">
                    <img src={logo} alt="" className="navbar-logo"/>
                    Orbit
                </Link>

                {liensVisibles.length > 0 && (
                    <nav className="navbar-links" aria-label="Navigation principale">
                        {liensVisibles.map((a) => (
                            <NavLink key={a.path} to={a.path}>
                                {a.labelParRole?.[utilisateur.role] ?? a.label}
                                {a.path === "/defaillances" && aTraiter > 0 && (
                                    <span className="nav-bulle" aria-label={`${aTraiter} à traiter`}>{aTraiter}</span>
                                )}
                            </NavLink>
                        ))}
                    </nav>
                )}

                <div className="navbar-user">
                    <div className="navbar-who">
                        {utilisateur.prenom} {utilisateur.nom}
                        <span className="navbar-role">{utilisateur.role}</span>
                    </div>
                    <ThemeButton />
                    <button className="navbar-logout" onClick={handleLogout}>
                        Déconnexion
                    </button>
                </div>
            </div>
        </header>
    );
}