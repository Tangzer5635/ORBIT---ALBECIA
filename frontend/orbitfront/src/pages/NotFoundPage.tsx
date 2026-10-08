import { Link } from "react-router-dom";

export function NotFoundPage() {
    return (
        <section className="empty-page">
            <p className="eyebrow">Erreur 404</p>
            <h1>Page inconnu</h1>
            <p>La route demandée n'existe pas.</p>
            <Link className="button button--primary" to="/">
                Retour
            </Link>
        </section>
    );
}