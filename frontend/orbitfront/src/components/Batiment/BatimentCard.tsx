import {Link} from "react-router-dom";
import type {Batiment} from "../../types/types.ts";
import Anneau from "../Utils/Anneau.tsx";

interface BatimentCardProps {
    batiment: Batiment;
    onSupprime: (batiment: Batiment) => void;
}

export default function BatimentCard({batiment, onSupprime}: BatimentCardProps) {
    const nbSalles = batiment.nbSalles; // hors salles de stockage

    return (
        <article className="bat-card">
            <header className="bat-card-head">
                <Anneau id={`bat-${batiment.id}`} comptes={batiment.anneau}/>
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

            <p className="bat-label muted">Salles de stockage</p>
            {batiment.stockages.map((s) => (
                <div key={s.id} className="bat-stock">
                    <div>
                        <div>Salle {s.numSalle}</div>
                        <div className="muted small">{s.nbMateriels} matériel{s.nbMateriels > 1 ? "s" : ""}</div>
                    </div>
                </div>
            ))}

            <div className="bat-actions bat-card-actions">
                <Link to={`/salles?batiment=${batiment.id}`} className="btn primary sm">Voir les salles</Link>
                <button type="button" className="btn sm danger" onClick={() => onSupprime(batiment)}>Supprimer</button>
            </div>
        </article>
    );
}