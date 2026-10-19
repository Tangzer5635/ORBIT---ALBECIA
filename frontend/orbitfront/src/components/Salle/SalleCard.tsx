import Anneau from "../Utils/Anneau.tsx";
import {LIBELLE_MOTIF_GEL, LIBELLE_TYPE_FORMATION, type SalleCarte} from "../../types/types.ts";
import {dateFr, pluriel} from "../../utils/postes.ts";
import { Link } from "react-router-dom";

interface SalleCardProps {
    salle: SalleCarte;
}

// "VU82, Réseaux" ou "Aucun stage"
function libelleStage(salle: SalleCarte): string {
    if (!salle.stageNom) return "Aucun stage";
    return salle.stageType ? `${salle.stageNom}, ${LIBELLE_TYPE_FORMATION[salle.stageType]}` : salle.stageNom;
}

export default function SalleCard({ salle }: SalleCardProps) {
    const { defectueux, aChanger } = salle.anneau;

    // problèmes à signaler, du plus grave au moins grave
    const alertes: { cle: string; texte: string }[] = [];
    if (defectueux) alertes.push({ cle: "def", texte: `${defectueux} défectueux` });
    if (aChanger) alertes.push({ cle: "chg", texte: `${aChanger} à changer bientôt` });

    return (
        <article className={`room${salle.suspendu ? " gel" : ""}`}>
            <Anneau id={salle.id} comptes={salle.anneau}/>
            <div>
                {/* le lien est dans le titre : le CSS l'étend à toute la carte */}
                <h3><Link to={`/salles/${salle.id}/`}>Salle {salle.numSalle}</Link></h3>
                <p>
                    {salle.numBatiment && `Bâtiment ${salle.numBatiment}, `}
                    {libelleStage(salle)}, {pluriel(salle.nbPostes, "poste")}
                </p>
                {salle.suspendu && (
                    <span className="gelb">
                        Gelée{salle.gelMotif && ` : ${LIBELLE_MOTIF_GEL[salle.gelMotif]}`}
                        {salle.gelJusqu && ` jusqu'au ${dateFr(salle.gelJusqu)}`}
                    </span>
                )}
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