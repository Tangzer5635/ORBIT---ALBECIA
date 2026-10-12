import BatimentCard from "../components/BatimentComponents.tsx";
import type {Batiment} from "../types/types.ts";
import {useEffect, useState} from "react";
import {getToken} from "../services/authService.ts";

export function BatimentsPage() {
    const [batiment, setBatiment] = useState<Batiment[]>([]);

    useEffect(() => {
        const reponse = fetch("http://localhost:8080/api/v1/batiments/", {
            headers: { Authorization: `Bearer ${getToken()}` },
        });

        reponse.then(response => response.json()).then(data => setBatiment(data.content)); //recuperer le contenu de la reponse (page)
    }, [])

    console.log(batiment);
    return (
        <main>
            <h1>Gestion Batiment</h1>
            <button>Créer un batiment</button>
            <div>
                {batiment.map((b) => (
                    <BatimentCard batiment={b} />
                ))}
            </div>

        </main>
    );
}
