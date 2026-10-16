import {useState} from "react";
import Popup from "../Popup.tsx";
import {LIBELLE_TYPE_MATERIEL, type Materiel} from "../../../types/types.ts";
import {MaterielError, modifierMateriel} from "../../../services/materielService.ts";

interface Props {
    materiel: Materiel;
    onClose: () => void;
    onFait: () => void;
}

// Le type ne se modifie pas : on garde celui du matériel
export default function ModifierMaterielPopup({materiel, onClose, onFait}: Props) {
    const [modele, setModele] = useState(materiel.modele);
    const [numSerie, setNumSerie] = useState(materiel.numSerie);
    const [dateAcquisition, setDateAcquisition] = useState(materiel.dateAcquisition);
    const [dateFinGarantie, setDateFinGarantie] = useState(materiel.dateFinGarantie);
    const [nomMachine, setNomMachine] = useState(materiel.nomMachine ?? "");
    const [erreur, setErreur] = useState("");
    const [enCours, setEnCours] = useState(false);

    const estUC = materiel.type === "UC";
    const complet = modele.trim() && numSerie.trim() && dateAcquisition && dateFinGarantie;

    async function valider() {
        if (!complet) return;
        setEnCours(true);
        setErreur("");
        try {
            await modifierMateriel(materiel.id, {
                type: materiel.type,
                modele: modele.trim(),
                numSerie: numSerie.trim(),
                dateAcquisition,
                dateFinGarantie,
                nomMachine: estUC ? nomMachine.trim() : undefined,
            });
            onFait();
        } catch (err) {
            setErreur(err instanceof MaterielError && err.detail ? err.detail : "Modification impossible.");
            setEnCours(false);
        }
    }

    return (
        <Popup
            open
            onClose={onClose}
            title={`Modifier : ${LIBELLE_TYPE_MATERIEL[materiel.type]}`}
            confirmLabel="Enregistrer"
            onConfirm={valider}
            loading={enCours}
            disabled={!complet}
        >
            <label htmlFor="modele">Modèle</label>
            <input id="modele" value={modele} onChange={e => setModele(e.target.value)} maxLength={50} autoFocus/>

            <label htmlFor="numSerie">N° de série</label>
            <input id="numSerie" value={numSerie} onChange={e => setNumSerie(e.target.value)} minLength={3}
                   maxLength={50}/>

            {estUC && (
                <>
                    <label htmlFor="nomMachine">Nom machine</label>
                    <input id="nomMachine" value={nomMachine} onChange={e => setNomMachine(e.target.value)}
                           maxLength={50}/>
                </>
            )}

            <label htmlFor="dateAcquisition">Date d'acquisition</label>
            <input id="dateAcquisition" type="date" value={dateAcquisition}
                   onChange={e => setDateAcquisition(e.target.value)}/>

            <label htmlFor="dateFinGarantie">Fin de garantie</label>
            <input id="dateFinGarantie" type="date" value={dateFinGarantie}
                   onChange={e => setDateFinGarantie(e.target.value)}/>

            {erreur && <p className="error-message" role="alert">{erreur}</p>}
        </Popup>
    );
}