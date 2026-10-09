import {type FormEvent, useState} from "react";
import "./LoginPage.css";


export function LoginPage() {
    const [identifiant, setIdentifiant] = useState("");
    const [error, setError] = useState("");

    function handleSubmit(event: FormEvent<HTMLFormElement>) {
        event.preventDefault();

        if(identifiant.trim() === "") {
            setError("Identifiant ou mot de passe incorrect.")
        }
    }

    return (
        <div className="login-page">

            <div className="login-card">
                <h1 className="login-title">Orbit</h1>
                <p className="login-subtitle">Outil de recensement des biens et inventaires techniques</p>

                <form className="login-form" onSubmit={handleSubmit}>
                    <label htmlFor="identifiant">Identifiant</label>
                    <input
                        type="text"
                        id="identifiant"
                        value={identifiant}
                        onChange={(e) => setIdentifiant(e.target.value)}
                    />

                    <label htmlFor="password">Mot de passe</label>
                    <input type="password" id="password" />

                    {error && <p className="error-message">{error}</p>}

                    <button type="submit" className="login-button">Se connecter</button>
                </form>
            </div>
        </div>
    );
}
