import type { ComptesAnneau } from "../types/types.ts";
import { CLASSE_ETAT, LIBELLE_ETAT, ORDRE_ETATS } from "../utils/postes.ts";

interface LegendeProps {
    comptes?: ComptesAnneau;
}

// Légende des couleurs de l'anneau (comme la maquette)
export default function Legende({ comptes }: LegendeProps) {
    return (
        <>
            <ul className={comptes ? "rleg" : "rleg compact"}>
                {ORDRE_ETATS.map((k) => (
                    <li key={k}>
                        <i className={"sw " + CLASSE_ETAT[k]}/>
                        {LIBELLE_ETAT[k]}
                        {comptes && <b>{comptes[k]}</b>}
                    </li>
                ))}
            </ul>
            {comptes && (
                <p className="rleg-note">
                    Le pourcentage correspond au matériel utilisable : tout sauf le défectueux
                    (bon état, libre en stock et à changer bientôt).
                </p>
            )}
        </>
    );
}