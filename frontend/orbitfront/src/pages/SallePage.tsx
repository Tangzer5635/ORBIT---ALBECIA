import { useEffect, useState } from "react";
import { useSearchParams } from "react-router-dom";
import { useBatiments } from "../context/BatimentsContext.tsx";
import { getSallesDuBatiment } from "../services/salleService.ts";
import { getUtilisateur } from "../services/authService.ts";
import Anneau from "../components/Anneau.tsx";
import Legende from "../components/Legende.tsx";
import SalleCard from "../components/SalleCard.tsx";
import StockageCard from "../components/StockageCard.tsx";
import type { SalleCarte } from "../types/types.ts";
import { pourcentageUtilisable, totalMateriels } from "../utils/postes.ts";

// résultat du dernier chargement, mémorisé avec le bâtiment concerné
interface Resultat {
    idBatiment: number;
    salles: SalleCarte[];
    erreur?: string;
}

type Affichage = "dispo";

const pluriel = (n: number, mot: string) => `${n} ${mot}${n > 1 ? "s" : ""}`;

const parNumero = (a: SalleCarte, b: SalleCarte) => a.numSalle.localeCompare(b.numSalle, "fr", { numeric: true });

// les salles les moins disponibles d'abord, celles sans poste à la fin
function parDisponibilite(a: SalleCarte, b: SalleCarte) {
    const pa = pourcentageUtilisable(a.anneau) ?? 101;
    const pb = pourcentageUtilisable(b.anneau) ?? 101;
    return pa - pb || parNumero(a, b);
}

export default function SallePage() {
    const [params] = useSearchParams();
    const { batiments } = useBatiments();
    const idBatiment = Number(params.get("batiment"));
    const batiment = batiments.find((b) => b.id === idBatiment);
    const role = getUtilisateur()?.role;

    const [resultat, setResultat] = useState<Resultat>();
    const [affichage, setAffichage] = useState<Affichage>("dispo"); // par disponibilité par défaut

    // recharge les salles à chaque changement de bâtiment
    useEffect(() => {
        if (!idBatiment) return;
        let actif = true; // ignore la réponse si on a changé de bâtiment entre-temps
        getSallesDuBatiment(idBatiment)
            .then((salles) => { if (actif) setResultat({ idBatiment, salles }); })
            .catch(() => {
                if (actif) setResultat({ idBatiment, salles: [], erreur: "Impossible de charger les salles de ce bâtiment." });
            });
        return () => { actif = false; };
    }, [idBatiment]);

    if (!idBatiment) {
        return (
            <>
                <div className="page-head"><h1>Salles</h1></div>
                <p className="muted">Choisis un bâtiment dans la barre pour voir ses salles.</p>
            </>
        );
    }

    // tant que le résultat concerne un autre bâtiment, on est en chargement
    const chargement = resultat?.idBatiment !== idBatiment;
    const salles = chargement ? [] : resultat.salles;
    const erreur = chargement ? undefined : resultat.erreur;

    const cours = salles.filter((s) => s.type === "SalleDeCours");
    const stockages = salles.filter((s) => s.type === "SalleStockage").sort(parNumero);
    // salles sous 80 % de postes utilisables (sans compter celles sans poste)
    const enDifficulte = cours.filter((s) => (pourcentageUtilisable(s.anneau) ?? 100) < 80).length;

    return (
        <>
            {batiment && (
                <section className="hero">
                    <Anneau id={`hero-${batiment.id}`} comptes={batiment.anneau} grand/>
                    <div>
                        <h1>Bâtiment {batiment.numBatiment}</h1>
                        <p>
                            {pluriel(totalMateriels(batiment.anneau), "matériel")}
                            dans {pluriel(batiment.nbSalles, "salle")}
                            {enDifficulte > 0 && `, dont ${enDifficulte} sous 80 % de matériel utilisable`}.
                        </p>
                        {(role === "ADMINISTRATEUR" || role === "GESTIONNAIRE") && (
                            <div className="hero-row">
                                <button type="button" className="btn primary">Créer une salle</button>
                                <button type="button" className="btn">Importer une salle</button>
                                {role === "ADMINISTRATEUR" && (
                                    <button type="button" className="btn">Modifier le bâtiment</button>
                                )}
                            </div>
                        )}
                    </div>
                </section>
            )}

            <div className="sh">
                <h2>Salles</h2>
            </div>
            <Legende/>

            {erreur && <p className="error-message" role="alert">{erreur}</p>}
            {chargement && <p className="muted">Chargement des salles…</p>}
            {!chargement && !erreur && salles.length === 0 && (
                <p className="muted">Aucune salle dans ce bâtiment.</p>
            )}

            {affichage === "dispo" && cours.length > 0 && (
                <div className="rooms">
                    {[...cours].sort(parDisponibilite).map((s) => <SalleCard key={s.id} salle={s}/>)}
                </div>
            )}
            {stockages.length > 0 && (
                <section className="floor">
                    <h2>Stockage</h2>
                    <div className="rooms">
                        {stockages.map((s) => <StockageCard key={s.id} salle={s}/>)}
                    </div>
                </section>
            )}
        </>
    );
}