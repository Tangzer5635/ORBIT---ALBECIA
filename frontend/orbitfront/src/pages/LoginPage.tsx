import { type FormEvent, useState } from "react";
import { useNavigate } from "react-router-dom";
import {AuthError, login} from "../services/authService.ts";
import logo from "../assets/orbit-logo.gif";

// Messages de la FD "S'authentifier"
function messageErreur(err: unknown): string {
    if (err instanceof AuthError) {
        if (err.status === 401) return "Identifiant ou mot de passe incorrect.";
        if (err.status === 423) return "Compte verrouillé. Contactez l'administrateur.";
        if (err.status === 403) return "Compte désactivé. Contactez l'administrateur.";
    }
    return "Le service est indisponible. Réessayez plus tard.";
}

export function LoginPage() {
    const [identifiant, setIdentifiant] = useState("");
    const [motDePasse, setMotDePasse] = useState("");
    const [error, setError] = useState("");
    const [chargement, setChargement] = useState(false);
    const navigate = useNavigate();

    async function handleSubmit(event: FormEvent<HTMLFormElement>) {
        event.preventDefault();

        // Champs obligatoires manquants
        if (identifiant.trim() === "" || motDePasse === "") {
            setError("Saisissez votre identifiant et votre mot de passe.");
            return;
        }

        setError("");
        setChargement(true);
        try {
            await login(identifiant.trim(), motDePasse);
            navigate("/accueil");
        } catch (err) {
            setError(messageErreur(err));
            setMotDePasse("");
        } finally {
            setChargement(false);
        }
    }

    return (
        <div className="login-page">
            <div className="login-card">
                <img src={logo} alt="" className="navbar-logo"/>
                <h1 className="login-title">Orbit</h1>
                <p className="login-subtitle">Outil de recensement des biens et inventaires techniques</p>

                <form className="login-form" onSubmit={handleSubmit} noValidate>
                    <label htmlFor="identifiant">Identifiant</label>
                    <input
                        type="text"
                        id="identifiant"
                        autoComplete="username"
                        value={identifiant}
                        onChange={(e) => setIdentifiant(e.target.value)}
                    />

                    <label htmlFor="password">Mot de passe</label>
                    <input
                        type="password"
                        id="password"
                        autoComplete="current-password"
                        value={motDePasse}
                        onChange={(e) => setMotDePasse(e.target.value)}
                    />

                    {error && <p className="error-message" role="alert">{error}</p>}

                    <button type="submit" className="login-button" disabled={chargement}>
                        {chargement ? "Connexion…" : "Se connecter"}
                    </button>
                </form>
            </div>
        </div>
    );
}
