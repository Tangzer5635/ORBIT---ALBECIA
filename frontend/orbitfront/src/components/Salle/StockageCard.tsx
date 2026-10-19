import type { SalleCarte } from "../../types/types.ts";
import {libelleEtage, pluriel, totalAnneau} from "../../utils/postes.ts";

interface StockageCardProps {
    salle: SalleCarte;
    // actions absentes pour qui ne gère pas les salles
    onModifier?: () => void;
    onConvertir?: () => void;
}


export default function StockageCard({ salle, onModifier, onConvertir }: StockageCardProps) {
    const { libre, defectueux, aChanger } = salle.anneau;
    const total = totalAnneau(salle.anneau);

    return (
        <article className="room stock">
            <div>
                <h3>Réserve</h3>
                <p>Salle {salle.numSalle} ({libelleEtage(salle.etage).toLowerCase()}), {pluriel(total, "matériel")}, dont {libre} disponible{libre > 1 ? "s" : ""}</p>
                {(defectueux > 0 || aChanger > 0) && (
                    <ul className="room-flags">
                        {defectueux > 0 && <li className="def">{defectueux} défectueux</li>}
                        {aChanger > 0 && <li className="chg">{aChanger} à changer bientôt</li>}
                    </ul>
                )}
                {(onModifier || onConvertir) && (
                    <div className="hero-row">
                        {onModifier && <button type="button" className="btn sm" onClick={onModifier}>Modifier</button>}
                        {onConvertir && <button type="button" className="btn sm" onClick={onConvertir}>Convertir en salle de cours</button>}
                    </div>
                )}
            </div>
        </article>
    );
}