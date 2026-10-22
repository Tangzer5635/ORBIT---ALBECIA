import {type ReactNode, useId, useState} from "react";

type Props = {
    titre: string;
    compte?: number;          // affiché entre parenthèses à côté du titre
    ouvertParDefaut?: boolean;
    children: ReactNode;      // monté seulement une fois la catégorie dépliée
};

// Catégorie dépliable du panneau d'un poste : un clic sur le titre déplie ou replie son contenu
export default function Repliable({titre, compte, ouvertParDefaut = false, children}: Props) {
    const [ouvert, setOuvert] = useState(ouvertParDefaut);
    const id = useId();

    return (
        <section className="panneau-section repliable">
            <button type="button" className="repliable-titre" aria-expanded={ouvert} aria-controls={id}
                    onClick={() => setOuvert(o => !o)}>
                <h3>{titre}{compte !== undefined && <span className="muted"> ({compte})</span>}</h3>
                <svg className={"repliable-chevron" + (ouvert ? " ouvert" : "")} viewBox="0 0 24 24" fill="none"
                     stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"
                     aria-hidden="true">
                    <path d="M6 9l6 6 6-6"/>
                </svg>
            </button>
            {ouvert && <div id={id} className="repliable-corps">{children}</div>}
        </section>
    );
}
