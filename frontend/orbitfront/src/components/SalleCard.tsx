import Anneau from "./Anneau.tsx";
import type { Salle } from "../types/types.ts";
import { libelleEtage } from "../utils/postes.ts";

interface SalleCardProps {
    salle: Salle;
}

function pluriel(n: number, mot: string) {
    return `${n} ${mot}${n > 1 ? "s" : ""}`;
}

export default function SalleCard({ salle }: SalleCardProps) {
    const { def, chg, inc } = salle.comptes;

    // problèmes à signaler, du plus grave au moins grave
    const alertes: { cle: string; texte: string }[] = [];
    if (def) alertes.push({ cle: "def", texte: `${def} défectueux` });
    if (chg) alertes.push({ cle: "chg", texte: `${chg} à changer bientôt` });
    if (inc) alertes.push({ cle: "inc", texte: `${inc} à compléter` });

    return (
        <article className="room">
            <Anneau id={salle.id} comptes={salle.comptes}/>
            <div>
                <h3>Salle {salle.numSalle}</h3>
                <p>{libelleEtage(salle.etage)}, {pluriel(salle.nbPostes, "poste")}</p>
                <ul className="room-flags">
                    {salle.nbPostes === 0 ? (
                        <li className="dispo">Aucun poste</li>
                    ) : alertes.length === 0 ? (
                        <li className="use">Tout est en ordre</li>
                    ) : (
                        alertes.map((a) => <li key={a.cle} className={a.cle}>{a.texte}</li>)
                    )}
                </ul>
            </div>
        </article>
    );
}
