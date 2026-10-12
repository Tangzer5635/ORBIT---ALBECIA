import { NavLink, useNavigate } from "react-router-dom";
import { getUtilisateur, logout } from "../services/authService.ts";
import type { Role } from "../types/types.ts";

// tableau avec les liens et roles pouvant y acceder
const LIENS: { to: string; label: string; roles: Role[] }[] = [
    { to: "/batiments",    label: "Bâtiments",    roles: ["GESTIONNAIRE"] },
    { to: "/defaillances", label: "Défaillances", roles: ["GESTIONNAIRE"] },
    { to: "/garanties",    label: "Garanties",    roles: ["GESTIONNAIRE"] },
    { to: "/materiel",     label: "Matériel",     roles: ["GESTIONNAIRE"] },
    { to: "/masters",      label: "Masters",      roles: ["GESTIONNAIRE"] },
    { to: "/stages",       label: "Stages",       roles: ["GESTIONNAIRE"] },
];

export default function Navbar() {
    const navigate = useNavigate();
    const utilisateur = getUtilisateur();

    //si personne n'est connecté, n'affiche pas la navbar
    if (!utilisateur) return null;

    // on filtre les liens par rapport au rôle de l'utilisateur
    const liensVisibles = LIENS.filter((lien) => lien.roles.includes(utilisateur.role));

    function handleLogout() {
        logout();
        navigate("/connexion");
    }

    return (
        <header className="navbar">
            <strong>Orbit</strong>

            {liensVisibles.length > 0 && (
                <nav>
                    {liensVisibles.map((lien) => (
                        <NavLink key={lien.to} to={lien.to}>
                            {lien.label}
                        </NavLink>
                    ))}
                </nav>
            )}

            <span>{utilisateur.prenom} {utilisateur.nom} {utilisateur.role}</span>
            {/*TODO bouton mode clair/sombre*/}
            <button onClick={handleLogout}>Déconnexion</button>
        </header>
    );
}