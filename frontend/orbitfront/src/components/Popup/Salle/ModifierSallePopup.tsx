import {type ChangeEvent, useState} from "react";
import Popup from "../Popup.tsx";
import type {Poste, Salle} from "../../../types/types.ts";
import {modifierSalle, SalleError} from "../../../services/salleService.ts";
import {estFormateur, libelleEtage, numeroPoste, pluriel} from "../../../utils/postes.ts";

interface Props {
    salle: Salle;
    postes: Poste[];
    onClose: () => void;
    onFait: (manquants: Partial<Record<string, number>>) => void; // ce qui n'était pas en stock
}

// Composition de la salle : postes stagiaires, vidéoprojecteurs, tableaux interactifs
export default function ModifierSallePopup({salle, postes, onClose, onFait}: Props) {
    const stagiaires = postes.filter(p => !estFormateur(p)).sort((a, b) => numeroPoste(a) - numeroPoste(b));
    const [nbPostes, setNbPostes] = useState(stagiaires.length);
    const [nbVideoProjecteurs, setNbVideoProjecteurs] = useState(salle.materielsParType.VideoProjecteur ?? 0);
    const [nbTableaux, setNbTableaux] = useState(salle.materielsParType.TableauInteractif ?? 0);
    const [erreur, setErreur] = useState("");
    const [enCours, setEnCours] = useState(false);

    // les derniers postes disparaissent si on en enlève
    const retires = stagiaires.slice(nbPostes);
    const occupesRetires = retires.filter(p => p.occupant).length;

    async function valider() {
        setEnCours(true);
        setErreur("");
        try {
            const {manquants} = await modifierSalle(salle.id, {nbPostes, nbVideoProjecteurs, nbTableaux});
            onFait(manquants);
        } catch (err) {
            setErreur(err instanceof SalleError && err.detail ? err.detail : "Modification impossible.");
            setEnCours(false);
        }
    }

    const nombre = (setter: (n: number) => void, max: number) =>
        (e: ChangeEvent<HTMLInputElement>) => setter(Math.min(max, Math.max(0, Number(e.target.value))));

    return (
        <Popup
            open
            onClose={onClose}
            title={`Modifier la salle ${salle.numSalle}`}
            description={`${libelleEtage(salle.etage)}. Le matériel en trop retourne au stock du bâtiment, le matériel manquant est pris dans le stock.`}
            confirmLabel="Enregistrer"
            onConfirm={valider}
            loading={enCours}
        >
            <label htmlFor="nbPostes">Postes stagiaire</label>
            <input id="nbPostes" type="number" min={0} max={50} value={nbPostes}
                   onChange={nombre(setNbPostes, 50)} autoFocus/>

            <label htmlFor="nbVp">Vidéoprojecteurs</label>
            <input id="nbVp" type="number" min={0} max={5} value={nbVideoProjecteurs}
                   onChange={nombre(setNbVideoProjecteurs, 5)}/>

            <label htmlFor="nbTbi">Tableaux interactifs</label>
            <input id="nbTbi" type="number" min={0} max={5} value={nbTableaux}
                   onChange={nombre(setNbTableaux, 5)}/>

            {retires.length > 0 && (
                <p className="error-message">
                    {retires.length > 1
                        ? `Les postes ${numeroPoste(retires[0])} à ${numeroPoste(retires[retires.length - 1])} seront supprimés`
                        : `Le poste ${numeroPoste(retires[0])} sera supprimé`}
                    , leur matériel retourne au stock.
                    {occupesRetires > 0 && ` ${pluriel(occupesRetires, "stagiaire perdra son poste", "stagiaires perdront leur poste")}.`}
                </p>
            )}
            {erreur && <p className="error-message" role="alert">{erreur}</p>}
        </Popup>
    );
}