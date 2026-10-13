import { LIBELLE_TYPE_MATERIEL, type Salle, type TypeMateriel } from "../types/types.ts";

interface StockageCardProps {
    salle: Salle;
}

// "Écran" -> "3 écrans", "Unité centrale" -> "2 unités centrales"
function quantite(n: number, type: TypeMateriel) {
    const libelle = LIBELLE_TYPE_MATERIEL[type].toLowerCase();
    if (n <= 1) return `${n} ${libelle}`;
    return `${n} ${libelle.split(" ").map((mot) => mot.endsWith("au") ? mot + "x" : mot + "s").join(" ")}`;
}

export default function StockageCard({ salle }: StockageCardProps) {
    const types = Object.entries(salle.materielsParType) as [TypeMateriel, number][];
    const total = salle.materielsId.length;

    return (
        <article className="room stock">
            <div>
                <h3>Réserve</h3>
                <p>Salle {salle.numSalle}, {total} matériel{total > 1 ? "s" : ""} disponible{total > 1 ? "s" : ""}</p>
                {types.length > 0 && (
                    <ul className="room-flags">
                        {types.map(([type, n]) => <li key={type}>{quantite(n, type)}</li>)}
                    </ul>
                )}
            </div>
        </article>
    );
}
