import {useState, type KeyboardEvent} from "react";
import MaterielDetailPopup from "../Popup/Materiel/MaterielDetailPopup.tsx";
import {LIBELLE_ETAT_MATERIEL, LIBELLE_TYPE_MATERIEL, type Materiel} from "../../types/types.ts";
import {CLASSE_ETAT_MATERIEL, localisationDe} from "../../utils/materiels.ts";
import {dateFr} from "../../utils/postes.ts";

interface Props {
    materiel: Materiel;
    onChange: () => void; // le matériel a changé : la liste se recharge
}

// Une ligne de la liste du matériel : un clic ouvre sa fiche (détails, défaillances, actions)
export default function MaterielLigne({materiel: m, onChange}: Props) {
    const [ouvert, setOuvert] = useState(false);

    const localisation = localisationDe(m);
    const archive = localisation === "ARCHIVE";
    const enStock = localisation === "STOCK";
    const aujourdhui = new Date().toLocaleDateString("sv-SE"); // "2026-10-15"

    const emplacement = archive
        ? `Archivé le ${m.dateArchivage ? dateFr(m.dateArchivage) : "?"}`
        : enStock ? `En stock, salle ${m.numSalle ?? "?"}` : `Salle ${m.numSalle ?? "?"}`;

    const onKeyDown = (e: KeyboardEvent<HTMLDivElement>) => {
        if (e.key === "Enter" || e.key === " ") {
            e.preventDefault();
            setOuvert(true);
        }
    };

    return (
        <>
            <div
                className={`li li-cliquable ${archive ? "dispo" : CLASSE_ETAT_MATERIEL[m.etat]}`}
                role="button"
                tabIndex={0}
                onClick={() => setOuvert(true)}
                onKeyDown={onKeyDown}>
                <strong>{LIBELLE_TYPE_MATERIEL[m.type]}, {m.modele}</strong>
                <strong>{LIBELLE_ETAT_MATERIEL[m.etat]}</strong>

                <span>
                    N° {m.numSerie}{m.nomMachine && `, ${m.nomMachine}`}, acquis le {dateFr(m.dateAcquisition)}
                </span>
                <span>{emplacement}</span>

                <span>
                    Garantie {m.dateFinGarantie < aujourdhui ? "terminée le " : "jusqu'au "}{dateFr(m.dateFinGarantie)}
                </span>
            </div>

            {ouvert && (
                <MaterielDetailPopup materiel={m} onClose={() => setOuvert(false)} onChange={onChange}/>
            )}
        </>
    );
}