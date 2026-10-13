import type { Comptes } from "../types/types.ts";
import { LIBELLE_ETAT_POSTE, ORDRE_ETATS } from "../utils/postes.ts";

interface LegendeProps {
    // avec comptes : affiche le nombre de postes de chaque état + la phrase d'explication
    comptes?: Comptes;
}

// Légende des couleurs de l'anneau (comme la maquette)
export default function Legende({ comptes }: LegendeProps) {
    return (
        <>
            <ul className={comptes ? "rleg" : "rleg compact"}>
                {ORDRE_ETATS.map((k) => (
                    <li key={k}>
                        <i className={"sw " + k}/>
                        {LIBELLE_ETAT_POSTE[k]}
                        {comptes && <b>{comptes[k]}</b>}
                    </li>
                ))}
            </ul>
            {comptes && (
                <p className="rleg-note">
                    Le pourcentage correspond à la partie verte et jaune du cercle : postes utilisés,
                    disponibles (vert hachuré) et à changer bientôt.
                </p>
            )}
        </>
    );
}