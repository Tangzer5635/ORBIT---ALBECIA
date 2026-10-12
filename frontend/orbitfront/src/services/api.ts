import { getToken } from "./authService.ts";


// En-tetes avec le token pour les routes protégées
export function authHeaders(): HeadersInit {
    return {
        "Content-Type": "application/json",
        Authorization: `Bearer ${getToken()}`,
    };
}