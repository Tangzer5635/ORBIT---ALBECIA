import type {ReactNode} from "react";
import {LIBELLE_ETAT_MATERIEL, LIBELLE_TYPE_MATERIEL, type Materiel} from "../../types/types.ts";
import {CLASSE_ETAT_MATERIEL} from "../../utils/materiels.ts";
import {dateFr} from "../../utils/postes.ts";

interface Props {
    materiel: Materiel;
    children?: ReactNode; // ce qui suit les infos : défaillance, actions...
}

// Carte d'infos d'un matériel, la même dans le panneau d'un poste (MaterielCard) et la fiche de la liste (MaterielDetailPopup)
export default function InfosMateriel({materiel: m, children}: Props) {
    const aujourdhui = new Date().toLocaleDateString("sv-SE"); // "2026-10-15"
    const archive = m.etat === "ARCHIVE";

    return (
        <article className={`carte-mat ${archive ? "dispo" : CLASSE_ETAT_MATERIEL[m.etat]}`}>
            <header>
                <b>{LIBELLE_TYPE_MATERIEL[m.type]}</b>
                <span className="pastille">{LIBELLE_ETAT_MATERIEL[m.etat]}</span>
            </header>

            <dl>
                <dt>Modèle</dt>
                <dd>{m.modele}</dd>
                <dt>N° de série</dt>
                <dd className="mono">{m.numSerie}</dd>
                {/* UC : nom machine et adresse MAC servent à l'Agent ORBIT, on montre aussi quand ils manquent */}
                {m.type === "UC" && (
                    <>
                        <dt>Nom machine</dt>
                        <dd className="mono">{m.nomMachine || <span className="muted">Non renseigné</span>}</dd>
                        <dt>Adresse MAC</dt>
                        <dd className="mono">{m.adresseMac || <span className="muted">Non renseignée</span>}</dd>
                    </>
                )}
                <dt>Acquis le</dt>
                <dd>{dateFr(m.dateAcquisition)}</dd>
                <dt>Garantie</dt>
                <dd>{m.dateFinGarantie < aujourdhui ? "Terminée le " : "Jusqu'au "}{dateFr(m.dateFinGarantie)}</dd>
            </dl>

            {children}
        </article>
    );
}
