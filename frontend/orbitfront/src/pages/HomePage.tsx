import { useNavigate } from "react-router-dom";
import { getUtilisateur, logout } from "../services/authService.ts";

//TODO Page provisoire pour tester la connexion.
export function HomePage() {
  const utilisateur = getUtilisateur();
  const navigate = useNavigate();

  function handleLogout() {
    logout();
    navigate("/connexion");
  }

  return (
    <main style={{ padding: 24 }}>
      <h1>Bonjour {utilisateur?.prenom}</h1>
      <p>Rôle : {utilisateur?.role}</p>
      <button onClick={handleLogout}>Déconnexion</button>
    </main>
  );
}
