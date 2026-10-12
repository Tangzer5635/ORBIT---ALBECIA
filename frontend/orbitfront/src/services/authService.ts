const API_URL = "http://localhost:8080/api/v1/auth";
const TOKEN_KEY = "orbit-token";
const USER_KEY = "orbit-user";

export type Role = "ADMINISTRATEUR" | "GESTIONNAIRE" | "FORMATEUR" | "STAGIAIRE";

export interface Utilisateur {
  id: number;
  prenom: string;
  nom: string;
  role: Role;
}

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
    console.log(response);
  } catch {
    throw new AuthError(0); // serveur injoignable
  }

  if (!response.ok) throw new AuthError(response.status);

  const data: { accessToken: string; utilisateur: Utilisateur } = await response.json();
  sessionStorage.setItem(TOKEN_KEY, data.accessToken);
  sessionStorage.setItem(USER_KEY, JSON.stringify(data.utilisateur));
  return data.utilisateur;
}

export function logout() {
  sessionStorage.removeItem(TOKEN_KEY);
  sessionStorage.removeItem(USER_KEY);
}

export function getToken(): string | null {
  return sessionStorage.getItem(TOKEN_KEY);
}

export function getUtilisateur(): Utilisateur | null {
  const json = sessionStorage.getItem(USER_KEY);
  return json ? JSON.parse(json) : null;
}

export function isConnecte(): boolean {
  return getToken() !== null;
}
