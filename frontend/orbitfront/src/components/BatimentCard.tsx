import {Link} from "react-router-dom";
import type {Batiment} from "../types/types.ts";
import Anneau from "./Anneau.tsx";

interface BatimentCardProps {
    batiment: Batiment;
    onSupprime: (id: number) => void;
}

export default function BatimentCard({batiment, onSupprime}: BatimentCardProps) {
    const nbSalles = batiment.nbSalles; // hors salles de stockage

    function handleSupprimer() {
        // Confirmation avant suppression
        if (window.confirm(`Supprimer le bâtiment ${batiment.numBatiment} ?`)) {
            onSupprime(batiment.id);
        }
    }

    return (
        <article className="bat-card">
            <header className="bat-card-head">
                <Anneau id={`bat-${batiment.id}`} comptes={batiment.comptes}/>
                <div>
                    <h2>
                        <Link to={`/salles?batiment=${batiment.id}`}>
                            Bâtiment {batiment.numBatiment} <span aria-hidden="true">›</span>
                        </Link>
                    </h2>
                </div>
            </header>

            <dl className="bat-infos">
                <dt>Salles</dt>
                <dd>{nbSalles} salle{nbSalles > 1 ? "s" : ""}</dd>
            </dl>

            <>
                <p className="bat-label muted">Salles de stockage</p>
                {batiment.stockages.length === 0 ? (
                    <p className="muted bat-vide">
                        Aucune : le matériel retiré des salles part dans la première réserve du parc.
                    </p>
                ) : (
                    batiment.stockages.map((s) => (
                        <div key={s.id} className="bat-stock">
                            <div>
                                <div>Salle {s.numSalle}</div>
                                <div className="muted small">{s.nbMateriels} matériel{s.nbMateriels > 1 ? "s" : ""}</div>
                            </div>
                        </div>
                    ))
                )}
            </>

            <div className="bat-actions bat-card-actions">
                <Link to={`/salles?batiment=${batiment.id}`} className="btn primary sm">Voir les salles</Link>
                <button type="button" className="btn sm">Modifier</button>
                <button type="button" className="btn sm danger" onClick={handleSupprimer}>Supprimer</button>
            </div>
        </article>
    );
}