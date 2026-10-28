import {useState} from "react";
import Popup from "../Popup.tsx";
import {MAC, LIBELLE_TYPE_MATERIEL, type TypeMateriel} from "../../../types/types.ts";
import {ajouterMateriel, MaterielError} from "../../../services/materielService.ts";
import {useBatiments} from "../../../context/BatimentsContext.tsx";
import {ORDRE_TYPES} from "../../../utils/materiels.ts";
import {dateFr} from "../../../utils/postes.ts";

interface Props {
    onClose: () => void;
    onFait: () => void; // matériel créé : la page se recharge
}

// date du jour au format "2026-10-15" (heure locale)
const aujourdhui = () => new Date().toLocaleDateString("sv-SE");

// "2026-10-15" + 2 ans -> "2028-10-15"
function plusAnnees(iso: string, annees: number) {
    const [a, m, j] = iso.split("-").map(Number);
    return new Date(a + annees, m - 1, j).toLocaleDateString("sv-SE");
}

// Le matériel est créé en stock ; on l'installe ensuite depuis la salle (« Affecter un matériel »)
export default function AjouterMaterielPopup({onClose, onFait}: Props) {
    const {batiments} = useBatiments();
    const [type, setType] = useState<TypeMateriel>("UC");
    const [modele, setModele] = useState("");
    const [numSerie, setNumSerie] = useState("");
    const [nomMachine, setNomMachine] = useState("");
    const [adresseMac, setAdresseMac] = useState("");
    const [dateAcquisition, setDateAcquisition] = useState(aujourdhui());
    const [garantie, setGarantie] = useState(2); // en années
    const [idStockage, setIdStockage] = useState(String(batiments.find(b => b.stockages.length)?.stockages[0]?.id ?? ""));
    const [erreur, setErreur] = useState("");
    const [enCours, setEnCours] = useState(false);

    const dateFinGarantie = dateAcquisition ? plusAnnees(dateAcquisition, garantie) : "";
    const macValide = type !== "UC" || !adresseMac.trim() || MAC.test(adresseMac.trim());
    const complet = modele.trim() && numSerie.trim().length >= 3 && dateAcquisition && idStockage && macValide;

    async function valider() {
        if (!complet) return;
        setEnCours(true);
        setErreur("");
        try {
            await ajouterMateriel({
                type,
                modele: modele.trim(),
                numSerie: numSerie.trim(),
                nomMachine: type === "UC" && nomMachine.trim() ? nomMachine.trim() : undefined,
                adresseMac: type === "UC" && adresseMac.trim() ? adresseMac.trim() : undefined,
                dateAcquisition,
                dateFinGarantie,
                idStockage: Number(idStockage),
            });
            onFait();
        } catch (err) {
            setErreur(err instanceof MaterielError && err.detail ? err.detail : "Création impossible.");
            setEnCours(false);
        }
    }

    return (
        <Popup
            open
            onClose={onClose}
            title="Ajouter un matériel"
            description="Le nouveau matériel est rangé en stock. Pour l'installer, allez ensuite dans la salle voulue et affectez-le au poste."
            confirmLabel="Ajouter"
            onConfirm={valider}
            loading={enCours}
            disabled={!complet}
        >
            <label htmlFor="type">Type</label>
            <select id="type" value={type} onChange={e => setType(e.target.value as TypeMateriel)} autoFocus>
                {ORDRE_TYPES.map(t => <option key={t} value={t}>{LIBELLE_TYPE_MATERIEL[t]}</option>)}
            </select>

            <label htmlFor="modele">Modèle</label>
            <input id="modele" value={modele} onChange={e => setModele(e.target.value)} maxLength={50}
                   placeholder="Ex. Dell OptiPlex 7010"/>

            <label htmlFor="numSerie">N° de série</label>
            <input id="numSerie" value={numSerie} onChange={e => setNumSerie(e.target.value)} minLength={3} maxLength={50}/>

            {type === "UC" && (
                <>
                    <label htmlFor="nomMachine">Nom machine (facultatif)</label>
                    <input id="nomMachine" value={nomMachine} onChange={e => setNomMachine(e.target.value)} maxLength={50}/>

                    <label htmlFor="adresseMac">Adresse MAC (facultatif)</label>
                    <input id="adresseMac" value={adresseMac} onChange={e => setAdresseMac(e.target.value)} maxLength={17}
                           placeholder="Ex. 00:0C:29:AB:CD:EF"/>
                    {!macValide && <p className="error-message" role="alert">Adresse MAC attendue, ex. 00:0C:29:AB:CD:EF.</p>}
                </>
            )}

            <label htmlFor="dateAcquisition">Date d'achat</label>
            <input id="dateAcquisition" type="date" value={dateAcquisition} onChange={e => setDateAcquisition(e.target.value)}/>

            <label htmlFor="garantie">Garantie (années)</label>
            <input id="garantie" type="number" min={0} max={10} value={garantie}
                   onChange={e => setGarantie(Math.max(0, Number(e.target.value)))}/>
            {dateFinGarantie && <p className="muted small">Garantie jusqu'au {dateFr(dateFinGarantie)}.</p>}

            <label htmlFor="stockage">Salle de stockage</label>
            <select id="stockage" value={idStockage} onChange={e => setIdStockage(e.target.value)}>
                <option value="" disabled>Choisir une salle de stockage…</option>
                {batiments.filter(b => b.stockages.length > 0).map(b => (
                    <optgroup key={b.id} label={`Bâtiment ${b.numBatiment}`}>
                        {b.stockages.map(s => <option key={s.id} value={s.id}>Salle {s.numSalle}</option>)}
                    </optgroup>
                ))}
            </select>

            {erreur && <p className="error-message" role="alert">{erreur}</p>}
        </Popup>
    );
}