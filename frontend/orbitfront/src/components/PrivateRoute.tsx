import { Navigate, Outlet } from "react-router-dom";
import { getUtilisateur } from "../services/authService.ts";
import { PAGE_ACCUEIL } from "../config/acces.ts";
import type { Role } from "../types/types.ts";

type Props = {
  roles?: Role[]; // si absent, il suffit d'être connecté
};

// Envoie l'utilisateur connecté vers la page de son role sinon le renvoie vers la page de connexion
export function RedirectionAccueil() {
  const utilisateur = getUtilisateur();
  if (!utilisateur) return <Navigate to="/connexion" replace />;
  return <Navigate to={PAGE_ACCUEIL[utilisateur.role]} replace />;
}

export function PrivateRoute({ roles }: Props) {
  const utilisateur = getUtilisateur();

  // pas connecté : retour a la page de connexion
  if (!utilisateur) return <Navigate to="/connexion" replace />;

  // role non autorisé : retour a la page de son role
  if (roles && !roles.includes(utilisateur.role)) {
    return <Navigate to={PAGE_ACCUEIL[utilisateur.role]} replace />;
  }

  return <Outlet />;
}