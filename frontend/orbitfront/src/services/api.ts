import { finDeSession, getToken, rafraichirToken } from "./authService.ts";

// En-tetes avec le token pour les routes protégées
export function authHeaders(): HeadersInit {
    return {
        "Content-Type": "application/json",
        Authorization: `Bearer ${getToken()}`,
    };
}

// Un seul refresh à la fois : les requêtes qui reçoivent un 401 en même temps attendent le même
let refreshEnCours: Promise<boolean> | null = null;

// fetch avec le token. Jeton expiré (401) : on demande un nouveau jeton puis on rejoue la requête une fois.
// Si le refresh est refusé, la session est terminée et on retourne à la page de connexion.
export async function apiFetch(url: string, init: RequestInit = {}): Promise<Response> {
    const envoyer = () => {
        const headers: Record<string, string> = {
            ...(init.headers as Record<string, string> | undefined),
            ...(authHeaders() as Record<string, string>),
        };
        // un fichier (FormData) : le navigateur met lui-même le Content-Type avec sa délimitation
        if (init.body instanceof FormData) delete headers["Content-Type"];
        return fetch(url, {...init, headers});
    };

    const response = await envoyer();
    if (response.status !== 401) return response;

    refreshEnCours ??= rafraichirToken().finally(() => { refreshEnCours = null; });
    if (await refreshEnCours) return envoyer();

    finDeSession();
    return response;
}