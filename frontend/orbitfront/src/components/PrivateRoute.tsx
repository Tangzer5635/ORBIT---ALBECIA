import { Navigate, Outlet } from "react-router-dom";
import { getUtilisateur } from "../services/authService.ts";
import type { Role } from "../types/types.ts";

type Props = {
  roles?: Role[]; // si absent : il suffit d'être connecté
};

export function PrivateRoute({ roles }: Props) {
  const utilisateur = getUtilisateur();

  // pas connecté : retour à la page de connexion
  if (!utilisateur) return <Navigate to="/connexion" replace />;

  // connecté mais rôle non autorisé : retour à l'accueil
  if (roles && !roles.includes(utilisateur.role)) return <Navigate to="/accueil" replace />;

  return <Outlet />;
}