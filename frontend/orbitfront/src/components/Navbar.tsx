import { Link, NavLink, useNavigate } from "react-router-dom";
import { getUtilisateur, logout } from "../services/authService.ts";
import logo from "../assets/orbit-logo.gif";
import { ACCES } from "../config/acces.ts";
import ThemeButton from "./ThemeButton.tsx";

export default function Navbar() {
    const navigate = useNavigate();
    const utilisateur = getUtilisateur();

    // si personne n'est connecté, pas de navbar (page de connexion)
    if (!utilisateur) return null;

    const liensVisibles = ACCES.filter((a) => a.roles.includes(utilisateur.role));

    function handleLogout() {
        logout();
        navigate("/connexion");
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