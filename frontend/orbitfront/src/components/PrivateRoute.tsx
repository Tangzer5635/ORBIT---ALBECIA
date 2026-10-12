import { Navigate, Outlet } from "react-router-dom";
import {isConnecte} from "../services/authService.ts";

// si on n'est pas connecté, retour à la page de connexion.
export function PrivateRoute() {
  if (!isConnecte()) return <Navigate to="/connexion" replace />;
  return <Outlet />;
}
