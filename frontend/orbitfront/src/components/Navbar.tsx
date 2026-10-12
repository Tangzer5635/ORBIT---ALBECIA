import { Link, NavLink, useNavigate } from "react-router-dom";
import { getUtilisateur, logout } from "../services/authService.ts";
import type { Role } from "../types/types.ts";
import "./Navbar.css";

// tableau avec les liens, le nom qui sera afficher sur la page et les roles pouvant y acceder
//TODO rajouter l'administrateur
const LIENS: { to: string; label: string; roles: Role[] }[] = [
    { to: "/batiments",    label: "Bâtiments",    roles: ["ADMINISTRATEUR"] },
    { to: "/salles",       label: "Salles",       roles: ["ADMINISTRATEUR","GESTIONNAIRE"]},
    { to: "/salles",       label: "Mes salles",   roles: ["FORMATEUR"] },
    { to: "/defaillances", label: "Défaillances", roles: ["GESTIONNAIRE", "FORMATEUR","ADMINISTRATEUR"] },
    { to: "/garanties",    label: "Garanties",    roles: ["GESTIONNAIRE","ADMINISTRATEUR"] },
    { to: "/materiel",     label: "Matériel",     roles: ["GESTIONNAIRE","ADMINISTRATEUR"] },
    { to: "/masters",      label: "Masters",      roles: ["GESTIONNAIRE","ADMINISTRATEUR"] },
    { to: "/stages",       label: "Stages",       roles: ["GESTIONNAIRE","ADMINISTRATEUR"] },
    { to: "/processus",    label: "Processus",    roles: ["FORMATEUR","ADMINISTRATEUR"] },
    { to: "/poste",        label: "Mon poste",    roles: ["STAGIAIRE"] },
    { to: "/utilisateurs", label: "Utilisateurs", roles: ["ADMINISTRATEUR"]},
    { to: "/journal", label: "Journal", roles: ["ADMINISTRATEUR"]},

];

export default function Navbar() {
    const navigate = useNavigate();
    const utilisateur = getUtilisateur();

    //si personne n'est connecté, n'affiche pas la navbar (pour la connexion)
    if (!utilisateur) return null;

    // on filtre les liens par rapport au rôle de l'utilisateur
    const liensVisibles = LIENS.filter((lien) => lien.roles.includes(utilisateur.role));

    function handleLogout() {
        logout();
        navigate("/connexion");
    }

    return (
        <header className="navbar">
            <div className="navbar-inner">
                <Link to="/accueil" className="navbar-brand">Orbit</Link>

                {liensVisibles.length > 0 && (
                    <nav className="navbar-links" aria-label="Navigation principale">
                        {liensVisibles.map((lien) => (
                            <NavLink key={lien.to} to={lien.to}>
                                {lien.label}
                            </NavLink>
                        ))}
                    </nav>
                )}

                <div className="navbar-user">
                    <div className="navbar-who">
                        {utilisateur.prenom} {utilisateur.nom}
                        <span className="navbar-role">{utilisateur.role}</span>
                    </div>
                    {/*TODO bouton mode clair/sombre*/}
                    <button className="navbar-logout" onClick={handleLogout}>
                        Déconnexion
                    </button>
                </div>
            </div>
        </header>
    );
}
