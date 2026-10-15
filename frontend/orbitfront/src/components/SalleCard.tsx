import Anneau from "./Anneau.tsx";
import {LIBELLE_MOTIF_GEL, type SalleCarte} from "../types/types.ts";
import {dateFr, libelleEtage, pluriel} from "../utils/postes.ts";
import { Link } from "react-router-dom";

interface SalleCardProps {
    salle: SalleCarte;
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
                <Link to={`/salles/${salle.id}/`}><h3>Salle {salle.numSalle}</h3></Link>
                <p>
                    {salle.numBatiment && `Bâtiment ${salle.numBatiment}, `}
                    {libelleEtage(salle.etage)}, {pluriel(salle.nbPostes, "poste")}
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