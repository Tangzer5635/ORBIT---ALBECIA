import type {Batiment} from "../types/types.ts";

interface BatimentCardProps {
    batiment: Batiment;
    onSupprime: (id: number) => void;
}

export default function BatimentCard({batiment, onSupprime}: BatimentCardProps) {
    function handleSupprimer() {
        // Confirmation avant suppression
        if (window.confirm(`Supprimer le bâtiment ${batiment.numBatiment} ?`)) {
            onSupprime(batiment.id);
        }
    }

    return (
        <>
            <h2>{batiment?.numBatiment}</h2>
            <button>Voir les salles</button>
            <button>Modifier</button>
            <button type={"button"} onClick={() => handleSupprimer()}>Supprimer</button>
        </>
    )
}