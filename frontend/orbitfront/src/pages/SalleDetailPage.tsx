import {useParams} from "react-router-dom";
import {getSalleById} from "../services/salleService.ts";
import type {Salle} from "../types/types.ts";
import {useEffect, useState} from "react";
import Anneau from "../components/Anneau.tsx";

export default function SalleDetailPage() {
    const {salleId} = useParams();
    const [salle, setSalle] = useState<Salle | null>();


    useEffect( () => {
        if (!salleId) return;
        getSalleById(salleId as string).then(setSalle).catch(console.error);

    }, [salleId])

    return (
        <main>
            <Anneau id={salleId} comptes={undefined}>
            </Anneau>
            <h1>Salle {salle?.numSalle}</h1>
            <h5>{salle?.etage},{salle?.nbPostes}</h5>
        </main>
    );
}