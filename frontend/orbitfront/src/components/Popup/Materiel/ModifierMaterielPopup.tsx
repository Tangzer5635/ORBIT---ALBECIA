import {useState} from "react";
import Popup from "../Popup.tsx";
import {MAC, LIBELLE_TYPE_MATERIEL, type Materiel} from "../../../types/types.ts";
import {MaterielError, modifierMateriel} from "../../../services/materielService.ts";
import {versPatch} from "../../../utils/patch.ts";

interface Props {
    materiel: Materiel;
    onClose: () => void;
    onFait: () => void;
}

// Ni le type ni le n° de série ne se modifient : seuls les champs changés partent dans le PATCH
export default function ModifierMaterielPopup({materiel, onClose, onFait}: Props) {
    const [modele, setModele] = useState(materiel.modele);
    const [dateAcquisition, setDateAcquisition] = useState(materiel.dateAcquisition);
    const [dateFinGarantie, setDateFinGarantie] = useState(materiel.dateFinGarantie);
    const [nomMachine, setNomMachine] = useState(materiel.nomMachine ?? "");
    const [adresseMac, setAdresseMac] = useState(materiel.adresseMac ?? "");
    const [erreur, setErreur] = useState("");
    const [enCours, setEnCours] = useState(false);

    const estUC = materiel.type === "UC";
    const macValide = !estUC || !adresseMac.trim() || MAC.test(adresseMac.trim());
    const complet = modele.trim() && dateAcquisition && dateFinGarantie && macValide;

    async function valider() {
        if (!complet) return;
        setEnCours(true);
        setErreur("");
        try {
            const operations = versPatch(
                {
                    modele: materiel.modele,
                    dateAcquisition: materiel.dateAcquisition,
                    dateFinGarantie: materiel.dateFinGarantie,
                    nomMachine: estUC ? materiel.nomMachine ?? "" : undefined,
                    adresseMac: estUC ? materiel.adresseMac ?? "" : undefined,
                },
                {
                    modele: modele.trim(),
                    dateAcquisition,
                    dateFinGarantie,
                    nomMachine: estUC ? nomMachine.trim() : undefined,
                    adresseMac: estUC ? adresseMac.trim() : undefined, // vide : l'adresse est effacée
                });
            if (operations.length) await modifierMateriel(materiel.id, operations);
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
            <input id="modele" value={modele} onChange={e => setModele(e.target.value)} maxLength={50}/>

            <label htmlFor="numSerie">N° de série</label>
            <input id="numSerie" value={materiel.numSerie} readOnly disabled
                   title="Le n° de série identifie le matériel : il ne se modifie pas"/>

            {estUC && (
                <>
                    <label htmlFor="nomMachine">Nom machine</label>
                    <input id="nomMachine" value={nomMachine} onChange={e => setNomMachine(e.target.value)}
                           maxLength={50}/>

                    <label htmlFor="adresseMac">Adresse MAC</label>
                    <input id="adresseMac" value={adresseMac} onChange={e => setAdresseMac(e.target.value)} maxLength={17}
                           placeholder="Ex. 00:0C:29:AB:CD:EF"/>
                    {!macValide && <p className="error-message" role="alert">Adresse MAC attendue, ex. 00:0C:29:AB:CD:EF.</p>}
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