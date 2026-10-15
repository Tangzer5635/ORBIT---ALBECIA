import { useEffect, useState } from "react";
import { getMesSalles } from "../services/salleService.ts";
import Legende from "../components/Legende.tsx";
import SalleCard from "../components/SalleCard.tsx";
import type { SalleCarte } from "../types/types.ts";

const parBatimentPuisNumero = (a: SalleCarte, b: SalleCarte) =>
    (a.numBatiment ?? "").localeCompare(b.numBatiment ?? "", "fr", { numeric: true })
    || a.numSalle.localeCompare(b.numSalle, "fr", { numeric: true });

// Page d'accueil du formateur : seulement les salles qui lui sont affectées
export default function MesSallesPage() {
    const [salles, setSalles] = useState<SalleCarte[]>();
    const [erreur, setErreur] = useState<string>();

    useEffect(() => {
        getMesSalles()
            .then((s) => setSalles([...s].sort(parBatimentPuisNumero)))
            .catch(() => setErreur("Impossible de charger tes salles."));
    }, []);

    return (
        <>
            <div className="page-head"><h1>Mes salles</h1></div>
            <Legende/>

            {erreur && <p className="error-message" role="alert">{erreur}</p>}
            {!salles && !erreur && <p className="muted">Chargement des salles…</p>}
            {salles?.length === 0 && <p className="muted">Aucune salle ne t'est affectée pour le moment.</p>}
            {salles && salles.length > 0 && (
                <div className="rooms">
                    {salles.map((s) => <SalleCard key={s.id} salle={s}/>)}
                </div>
            )}
        </>
    );
}
