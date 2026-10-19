import { useEffect, useState } from "react";
import { Navigate, useSearchParams } from "react-router-dom";
import { useBatiments } from "../context/BatimentsContext.tsx";
import { getSallesDuBatiment } from "../services/salleService.ts";
import { getUtilisateur } from "../services/authService.ts";
import Anneau from "../components/Utils/Anneau.tsx";
import Legende from "../components/Utils/Legende.tsx";
import SalleCard from "../components/Salle/SalleCard.tsx";
import StockageCard from "../components/Salle/StockageCard.tsx";
import type {SalleCarte} from "../types/types.ts";
import {pluriel, pourcentageUtilisable, totalAnneau} from "../utils/postes.ts";
import CreerSallePopup from "../components/Popup/Salle/CreerSallePopup.tsx";
import ModifierStockagePopup from "../components/Popup/Salle/ModifierStockagePopup.tsx";
import ConvertirStockagePopup from "../components/Popup/Salle/ConvertirStockagePopup.tsx";
import {getBatiments} from "../services/batimentService.ts";

// résultat du dernier chargement, mémorisé avec le bâtiment concerné
interface Resultat {
    idBatiment: number;
    salles: SalleCarte[];
    erreur?: string;
}

const parNumero = (a: SalleCarte, b: SalleCarte) => a.numSalle.localeCompare(b.numSalle, "fr", { numeric: true });

// les salles les moins disponibles d'abord, celles sans poste à la fin
function parDisponibilite(a: SalleCarte, b: SalleCarte) {
    const pa = pourcentageUtilisable(a.anneau) ?? 101;
    const pb = pourcentageUtilisable(b.anneau) ?? 101;
    return pa - pb || parNumero(a, b);
}

export default function SallePage() {
    const [params] = useSearchParams();
    const { batiments, setBatiments } = useBatiments();
    const [creerOuvert, setCreerOuvert] = useState(false);
    // salle de stockage dont on change le numéro / qu'on convertit en salle de cours
    const [aModifier, setAModifier] = useState<SalleCarte | null>(null);
    const [aConvertir, setAConvertir] = useState<SalleCarte | null>(null);
    const [version, setVersion] = useState(0);
    const idBatiment = Number(params.get("batiment"));
    const batiment = batiments.find((b) => b.id === idBatiment);
    const role = getUtilisateur()?.role;
    const peutGerer = role === "ADMINISTRATEUR" || role === "GESTIONNAIRE";

    const [resultat, setResultat] = useState<Resultat>();
    const [affichage] = useState<string>("dispo");
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
    }, [idBatiment, version]);

    function salleCreee() {
        setCreerOuvert(false);
        setAModifier(null);
        setAConvertir(null);
        setVersion((v) => v + 1);
        getBatiments().then(setBatiments).catch(() => {});
    }

    if (!idBatiment) {
        // admin et gestionnaire (qui ont la barre des bâtiments) arrivent directement sur le premier bâtiment
        if ((role === "ADMINISTRATEUR" || role === "GESTIONNAIRE") && batiments.length > 0) {
            return <Navigate to={`/salles?batiment=${batiments[0].id}`} replace/>;
        }
        return (
            <>
                <div className="page-head"><h1>Salles</h1></div>
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
                            {pluriel(totalAnneau(batiment.anneau), "poste")}
                            {" "} dans {pluriel(batiment.nbSalles, "salle")}
                            {enDifficulte > 0 && `, dont ${enDifficulte} sous 80 % de postes utilisables`}.
                        </p>
                        {(role === "ADMINISTRATEUR" || role === "GESTIONNAIRE") && (
                            <div className="hero-row">
                                <button type="button" className="btn primary" onClick={() => setCreerOuvert(true)}>Créer une salle</button>
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
                        {stockages.map((s) => (
                            <StockageCard key={s.id} salle={s}
                                          onModifier={peutGerer ? () => setAModifier(s) : undefined}
                                          onConvertir={peutGerer ? () => setAConvertir(s) : undefined}/>
                        ))}
                    </div>
                </section>
            )}
            {creerOuvert && batiment && (
                <CreerSallePopup batiment={batiment} onClose={() => setCreerOuvert(false)} onCree={salleCreee}/>
            )}
            {aModifier && (
                <ModifierStockagePopup salle={aModifier} onClose={() => setAModifier(null)} onFait={salleCreee}/>
            )}
            {aConvertir && (
                <ConvertirStockagePopup salle={aConvertir}
                                        autresStockages={stockages.filter((s) => s.id !== aConvertir.id)}
                                        onClose={() => setAConvertir(null)} onFait={salleCreee}/>
            )}
        </>
    );
}