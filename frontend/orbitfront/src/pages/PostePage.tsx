import {useEffect, useState} from "react";
import {LIBELLE_ETAT_MATERIEL, LIBELLE_TYPE_MATERIEL, type MonPoste} from "../types/types.ts";
import {getMyPoste} from "../services/posteService.ts";



export default function PostePage() {
    const [poste, setPoste] = useState<MonPoste | null>();
    const [erreurChargement, setErreurChargement] = useState<string>("");

    useEffect(() => {
        getMyPoste()
            .then(setPoste)
            .catch(() => setErreurChargement("Impossible de charger le poste."));
    }, []);

    if (erreurChargement) {
        return (
            <section>
                <h1>Mon poste</h1>
                <p className="error-message" role="alert">{erreurChargement}</p>
            </section>
        );
    }

    if (poste === undefined) return <section><p>Chargement…</p></section>;

    // Alternatif 1 : aucun poste affecté
    if (poste === null) {
        return (
            <section>
                <h1>Mon poste</h1>
                <p>Aucun poste ne vous est affecté pour le moment.</p>
            </section>
        );
    }

    return (
        <section>

            {/*Component du haut*/}
            <div>
                <h1>{poste.numPoste} poste</h1>
                <p>Salle {poste.numSalle}, Bâtiment {poste.numBatiment}</p>
                <button>Remonter une défaillance</button>
            </div>

            {/* Matériel du poste */}
            <h2>Mon matériel</h2>
            {poste.materiels.length === 0 ? (
                <p>Aucun matériel sur ce poste.</p>
            ) : (
                <ul>
                    {poste.materiels.map((m) => (
                        <li key={m.id}>
                            <div>
                                <strong>{LIBELLE_TYPE_MATERIEL[m.type]}</strong>
                                <span>
                                    {LIBELLE_ETAT_MATERIEL[m.etat]}
                                </span>
                            </div>
                            <p>Modèle {m.modele}</p>
                            <p>N° de série {m.numSerie} </p>
                            <p>Garantie jusqu'au {new Date(m.dateFinGarantie).toLocaleDateString("fr-FR")}</p>

                        </li>
                    ))}
                </ul>
            )}

            <h2>Où se trouve mon poste</h2>

            <h2>Mes défaillances remontées</h2>

        </section>
    );
}