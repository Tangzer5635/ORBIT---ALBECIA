import { getUtilisateur } from "../services/authService.ts";

//TODO Page provisoire pour tester la connexion.
export function HomePage() {
  const utilisateur = getUtilisateur();


  return (
    <main style={{ padding: 24 }}>
      <h1>Bonjour {utilisateur?.prenom}</h1>
      <p>Rôle : {utilisateur?.role}</p>
    </main>
  );
}
