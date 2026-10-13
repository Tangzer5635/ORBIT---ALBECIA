import type { SalleCarte } from "../types/types.ts";
import { totalMateriels } from "../utils/postes.ts";

interface StockageCardProps {
    salle: SalleCarte;
}

const pluriel = (n: number, mot: string) => `${n} ${mot}${n > 1 ? "s" : ""}`;

export default function StockageCard({ salle }: StockageCardProps) {
    const { libre, defectueux, aChanger } = salle.anneau;
    const total = totalMateriels(salle.anneau);

    return (
        <article className="room stock">
            <div>
                <h3>Réserve</h3>
                <p>Salle {salle.numSalle}, {pluriel(total, "matériel")}, dont {libre} disponible{libre > 1 ? "s" : ""}</p>
                {(defectueux > 0 || aChanger > 0) && (
                    <ul className="room-flags">
                        {defectueux > 0 && <li className="def">{defectueux} défectueux</li>}
                        {aChanger > 0 && <li className="chg">{aChanger} à changer bientôt</li>}
                    </ul>
                )}
            </div>
        </article>
    );
}