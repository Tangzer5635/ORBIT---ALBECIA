import {useEffect, useState} from "react";
import type {Poste} from "../types/types.ts";
import {getMyPoste} from "../services/posteService.ts";

export default function PostePage() {
    const [poste, setPoste] = useState<Poste | null>();
    const [erreurChargement, setErreurChargement] = useState<string>("");

    useEffect(() => {
        getMyPoste()
            .then(setPoste)
            .catch(() => setErreurChargement("Impossible de charger le poste."));
    }, []);

    return (
        <section>
            <h1>Mon poste</h1>

            {erreurChargement && <p className="error-message" role="alert">{erreurChargement}</p>}

            {!erreurChargement && poste === undefined && <p>Chargement…</p>}
            {poste === null && <p>Aucun poste ne vous est affecté.</p>}
            {poste && <h3>Poste {poste.numPoste}</h3>}
        </section>
    );
}