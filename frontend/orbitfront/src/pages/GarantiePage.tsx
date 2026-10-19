import {useEffect, useState} from "react";
import type {CalendrierGaranties} from "../types/types.ts";
import {getCalendrierGaranties} from "../services/materielService.ts";
import {pluriel} from "../utils/postes.ts";

// "2027-03" = "mars"
function nomMois(mois: string): string {
    const [annee, numero] = mois.split("-").map(Number);
    return new Date(annee, numero - 1, 1).toLocaleDateString("fr-FR", {month: "long"});
}

export default function GarantiePage() {
    const aujourdhui = new Date();
    const anneeCourante = aujourdhui.getFullYear();

    const [annee, setAnnee] = useState(anneeCourante);
    const [calendrier, setCalendrier] = useState<CalendrierGaranties>();
    const [erreur, setErreur] = useState("");

    // recharge à chaque changement d'année
    useEffect(() => {
        let actif = true; // ignore une réponse arrivée après un nouveau clic sur les flèches
        getCalendrierGaranties(annee)
            .then((c) => {
                if (actif) {
                    setCalendrier(c);
                    setErreur("");
                }
            })
            .catch(() => {
                if (actif) setErreur("Impossible de charger le calendrier des garanties.");
            });
        return () => { actif = false; };
    }, [annee]);

    // tant que la réponse concerne une autre année, on est en chargement
    const chargement = calendrier?.annee !== annee;
    const total = calendrier && !chargement ? calendrier.mois.reduce((somme, m) => somme + m.nb, 0) : 0;

    return (
        <>
            <div>
                <h1>Garanties</h1>
            </div>

            {calendrier && calendrier.expirees > 0 && (
                <p>{pluriel(calendrier.expirees, "matériel")} déjà hors garantie à ce jour.</p>
            )}

            <nav>
                <button type="button" onClick={() => setAnnee((a) => a - 1)}>‹ {annee - 1}</button>
                <h2>{annee}</h2>
                <button type="button" onClick={() => setAnnee((a) => a + 1)}>{annee + 1} ›</button>
            </nav>

            {erreur && <p>{erreur}</p>}
            {chargement && !erreur && <p>Chargement du calendrier…</p>}

            {calendrier && !chargement && (
                <>
                    <p>{pluriel(total, "fin")} de garantie en {annee}</p>

                    <ol>
                        {calendrier.mois.map((m) => (
                            <li key={m.mois}>
                                <p>{nomMois(m.mois)}</p>
                                <p>{m.nb === 0 ? "0" : pluriel(m.nb, "matériel")}</p>
                            </li>
                        ))}
                    </ol>
                </>
            )}
        </>
    );
}