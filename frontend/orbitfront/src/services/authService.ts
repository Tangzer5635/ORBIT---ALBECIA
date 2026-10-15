import type {TokenResponse, Utilisateur} from "../types/types.ts";

const API_URL = "/api/v1/auth";
const TOKEN_KEY = "orbit-token";
const USER_KEY = "orbit-user";



// Erreur avec le code HTTP, pour choisir le bon message dans la page.
export class AuthError extends Error {
  status: number;
  constructor(status: number) {
    super("Erreur d'authentification " + status);
    this.status = status;
  }
}

// Méthode pour se connecter
export async function login(username: string, password: string): Promise<Utilisateur> {
  let response: Response;
  try {
    response = await fetch(`${API_URL}/login/`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ username, password }),
    });
  } catch {
    throw new AuthError(0); // serveur injoignable
  }

  if (!response.ok) throw new AuthError(response.status);

  const data: TokenResponse = await response.json();
  sessionStorage.setItem(TOKEN_KEY, data.accessToken);
  sessionStorage.setItem(USER_KEY, JSON.stringify(data.utilisateur));
  return data.utilisateur;
}

// Méthode pour se déconnecter, la session locale est vidée dans tous les cas,
export async function logout(): Promise<void> {
  let response: Response;
  try {
    try {
      response = await fetch(`${API_URL}/logout/`, {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
          Authorization: `Bearer ${getToken()}`, // le serveur doit savoir quel token révoquer
        },
      });
    } catch {
      throw new AuthError(0); // serveur injoignable
    }

    if (!response.ok) throw new AuthError(response.status);
  } finally {
    sessionStorage.removeItem(TOKEN_KEY);
    sessionStorage.removeItem(USER_KEY);
  }
}

export function getToken(): string | null {
  return sessionStorage.getItem(TOKEN_KEY);
}

// Demande un nouveau jeton avec le cookie de refresh (HttpOnly, envoyé tout seul par le navigateur).
// false si le refresh est refusé : cookie expiré, compte désactivé ou verrouillé.
export async function rafraichirToken(): Promise<boolean> {
  try {
    const response = await fetch(`${API_URL}/refresh/`, { method: "POST" });
    if (!response.ok) return false;
    const data: TokenResponse = await response.json();
    sessionStorage.setItem(TOKEN_KEY, data.accessToken);
    return true;
  } catch {
    return false; // serveur injoignable
  }
}

// Session terminée (refresh refusé) : on vide la session et on retourne à la connexion
export function finDeSession() {
  sessionStorage.removeItem(TOKEN_KEY);
  sessionStorage.removeItem(USER_KEY);
  window.location.assign("/connexion?expiree");
}

export function getUtilisateur(): Utilisateur | null {
  const json = sessionStorage.getItem(USER_KEY);
  return json ? JSON.parse(json) : null;
}

export function isConnecte(): boolean {
  return getToken() !== null;
}