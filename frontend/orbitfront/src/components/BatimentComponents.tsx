import type {Batiment} from "../types/types.ts";

interface BatimentCardProps {
    batiment?: Batiment
}

export default function BatimentCard({batiment}: BatimentCardProps) {
    console.log(batiment)
    return (
        <>
            <h2>{batiment?.numBatiment}</h2>
            <button>Voir les salles</button>
            <button>Modifier</button>
            <button>Supprimer</button>
        </>
    )
}