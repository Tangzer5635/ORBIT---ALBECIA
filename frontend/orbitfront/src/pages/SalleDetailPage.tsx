import {Link, useParams} from "react-router-dom";
import {getPostesBySalle, getSalleById} from "../services/salleService.ts";
import {getUtilisateur} from "../services/authService.ts";
import type {EtatPoste, Poste, Salle} from "../types/types.ts";
import {useEffect, useState} from "react";
import Anneau from "../components/Anneau.tsx";
import {libelleEtage, pluriel} from "../utils/postes.ts";

// ordre des tuiles, libellé et classe de couleur (classes déjà dans index.css)
const ETATS: { etat: EtatPoste; libelle: string; classe: string }[] = [
    {etat: "DEFECTUEUX", libelle: "Défectueux", classe: "def"},
    {etat: "A_COMPLETER", libelle: "À compléter", classe: "inc"},
    {etat: "A_CHANGER", libelle: "Prévoir changement", classe: "chg"},
    {etat: "UTILISE", libelle: "Utilisé", classe: "use"},
    {etat: "DISPONIBLE", libelle: "Disponible", classe: "dispo"},
];

export default function SalleDetailPage() {
    const {salleId} = useParams();
    const [salle, setSalle] = useState<Salle | null>(null);
    const [postes, setPostes] = useState<Poste[]>([]);
    const [filtre, setFiltre] = useState<EtatPoste | null>(null);
    const role = getUtilisateur()?.role;
    const peutGerer = role === "ADMINISTRATEUR" || role === "GESTIONNAIRE";

    useEffect(() => {
        if (!salleId) return;
        setFiltre(null);
        getSalleById(salleId).then(setSalle).catch(console.error);
        getPostesBySalle(salleId).then(setPostes).catch(console.error);
    }, [salleId]);

    const compte = (e: EtatPoste) => postes.filter(p => p.etat === e).length;

    // l'anneau compte les postes : défectueux et à compléter = non utilisables
    const anneau = {
        bonEtat: compte("UTILISE"),
        libre: compte("DISPONIBLE"),
        aChanger: compte("A_CHANGER"),
        defectueux: compte("DEFECTUEUX") + compte("A_COMPLETER"),
    };

    if (!salle) return <p className="muted">Chargement de la salle…</p>;

    return (
        <>
            <Link to={-1 as unknown as string} className="muted small">‹ Retour</Link>

            <section className="hero">
                <Anneau id={`salle-${salle.id}`} comptes={anneau} grand/>
                <div>
                    <h1>Salle {salle.numSalle}</h1>
                    <p>{libelleEtage(salle.etage)}, {pluriel(salle.nbPostes, "poste")}.</p>

                    <div className="tuiles">
                        {ETATS.map(({etat, libelle, classe}) => (
                            <button key={etat} type="button" className={`tuile ${classe}`}
                                    aria-pressed={filtre === etat}
                                    onClick={() => setFiltre(filtre === etat ? null : etat)}>
                                <b>{compte(etat)}</b>
                                {libelle}
                            </button>
                        ))}
                    </div>
                    <p className="rleg-note">
                        Le cercle indique la part de postes utilisables : utilisés, disponibles et à changer bientôt.
                        Clique sur une case pour filtrer les postes.
                    </p>
                </div>
            </section>

            {peutGerer && (
                <div className="hero-row">
                    <button type="button" className="btn">Modifier la salle</button>
                    <button type="button" className="btn">Affecter un stage</button>
                    <button type="button" className="btn">Exporter la salle</button>
                    <button type="button" className="btn">Geler la salle</button>
                    <button type="button" className="btn danger">Supprimer la salle</button>
                </div>
            )}

            <ul className="postes">
                {postes.map(p => {
                    const info = ETATS.find(e => e.etat === p.etat);
                    return (
                        <li key={p.id} className={filtre && p.etat !== filtre ? "estompe" : undefined}>
                            <i className={`sw ${info?.classe ?? ""}`}/>
                            Poste {p.numPoste} : {info?.libelle ?? "—"}, {pluriel(p.materielsId.length, "matériel")}
                        </li>
                    );
                })}
            </ul>
        </>
    );
}