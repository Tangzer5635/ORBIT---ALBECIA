import {useEffect, useState} from "react";
import type {Materiel, Poste, Salle, TypeMateriel} from "../../types/types.ts";
import {getMaterielById} from "../../services/materielService.ts";
import MaterielCard from "../Materiel/MaterielCard.tsx";
import {estFormateur, etatPoste, nomPoste, pluriel} from "../../utils/postes.ts";
import {NOM_TYPE, ORDRE_TYPES} from "../../utils/materiels.ts";

type Props = {
    poste: Poste;
    salle: Salle;
    peutGerer: boolean; // gestionnaire ou admin : détail du matériel et actions sur le matériel
    // affecter / retirer un stagiaire : tout le rôle technique (formateur compris), donc toujours ouvert ici
    onFermer: () => void;
    onAffecterStagiaire?: () => void; // absent si la salle est gelée
    onRetirerStagiaire: () => void;
    onAffecterMateriel: () => void;
    onChange: () => void; // un matériel a bougé : la page se recharge
};

export default function PanneauPoste({
                                         poste, salle, peutGerer, onFermer, onAffecterStagiaire, onRetirerStagiaire,
                                         onAffecterMateriel, onChange
                                     }: Props) {
    const [materiels, setMateriels] = useState<Materiel[] | null>(null);
    const formateur = estFormateur(poste);
    const etat = etatPoste(poste.etat);

    // le détail du matériel n'est ouvert qu'au gestionnaire (le formateur voit seulement les types)
    useEffect(() => {
        if (!peutGerer) return;
        Promise.all(poste.materielsId.map(getMaterielById))
            .then(setMateriels)
            .catch(console.error);
    }, [poste, peutGerer]);

    const formateurs = (salle.formateurs ?? []).map(f => `${f.prenom} ${f.nom}`);
    const typesPoses = ORDRE_TYPES
        .filter(t => poste.materielsParType?.[t])
        .map((t: TypeMateriel) => pluriel(poste.materielsParType[t] ?? 0, ...NOM_TYPE[t]));

    return (
        <aside className="poste-panneau" aria-label={nomPoste(poste)}>
            <button type="button" className="muted small lien" onClick={onFermer}>‹ Vue de la salle</button>

            <div className={`panneau-entete ${etat?.classe ?? ""}`}>
                <div className="poste-entete">
                    <h2>{nomPoste(poste)}</h2>
                    {etat && <span className="pastille">{etat.libelle}</span>}
                </div>
                <p className="muted small">Salle {salle.numSalle}{salle.stageNom && `, ${salle.stageNom}`}</p>
            </div>

            {(peutGerer || (!formateur && !poste.occupant && onAffecterStagiaire)) && (
                <div className="poste-actions">
                    {!formateur && !poste.occupant && onAffecterStagiaire && (
                        <button type="button" className="btn primary" onClick={onAffecterStagiaire}>
                            Affecter un stagiaire
                        </button>
                    )}
                    {peutGerer && (
                        <button type="button" className="btn" onClick={onAffecterMateriel}>Affecter un matériel</button>
                    )}
                </div>
            )}

            {formateur ? (
                <section className="panneau-section">
                    <h3>Utilisé par</h3>
                    <p className={formateurs.length ? "" : "muted"}>
                        {formateurs.length ? formateurs.join(", ") : "Aucun formateur attribué à la salle"}
                    </p>
                </section>
            ) : (
                <section className="panneau-section">
                    <h3>Occupant</h3>
                    {poste.occupant ? (
                        <ul className="panneau-liste">
                            <li>
                                {poste.occupant}
                                <button type="button" className="muted small lien" onClick={onRetirerStagiaire}>
                                    Retirer du poste
                                </button>
                            </li>
                        </ul>
                    ) : (
                        <p className="muted">Aucun stagiaire : poste disponible</p>
                    )}
                </section>
            )}

            <section className="panneau-section">
                <h3>Matériel du poste</h3>
                {poste.materielsId.length === 0 ? (
                    <p className="muted">Aucun matériel sur ce poste.</p>
                ) : !peutGerer ? (
                    <p>{typesPoses.join(", ")}</p>
                ) : materiels === null ? (
                    <p className="muted">Chargement du matériel…</p>
                ) : (
                    materiels.map(m => (
                        <MaterielCard key={m.id} materiel={m} depuis={{posteId: poste.id}} onChange={onChange}/>
                    ))
                )}
            </section>
        </aside>
    );
}