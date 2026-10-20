import type {ReactNode} from "react";

interface PaginationProps {
    page: number;          // page affichée, commence à 0 (comme le back)
    totalPages: number;
    onChange: (page: number) => void;
    disabled?: boolean;    // ex. pendant un chargement
    info?: ReactNode;      // texte après le nombre de pages, ex. "(4458 matériels)"
}

// « ‹ [ 3 ] / 223 › » : première, précédente, saisie d'un numéro, suivante, dernière page
export default function Pagination({page, totalPages, onChange, disabled = false, info}: PaginationProps) {
    if (totalPages <= 1) return null;

    const premiere = page <= 0;
    const derniere = page >= totalPages - 1;

    // numéro tapé (à partir de 1) : ramené entre 1 et totalPages, ignoré s'il n'est pas un nombre
    function aller(input: HTMLInputElement) {
        const saisi = Number.parseInt(input.value, 10);
        if (Number.isNaN(saisi)) {
            input.value = String(page + 1);
            return;
        }
        const cible = Math.min(Math.max(saisi, 1), totalPages) - 1;
        input.value = String(cible + 1);
        if (cible !== page) onChange(cible);
    }

    return (
        <nav className="pagination" aria-label="Pagination">
            <button type="button" className="btn sm" disabled={disabled || premiere}
                    onClick={() => onChange(0)} title="Première page" aria-label="Première page">Premier</button>
            <button type="button" className="btn sm" disabled={disabled || premiere}
                    onClick={() => onChange(page - 1)} title="Page précédente" aria-label="Page précédente">Précédent</button>

            <span className="muted small pagination-saisie">
                {/* key = page : le champ reprend le bon numéro quand la page change autrement (boutons, filtres) */}
                <input key={page} type="number" min={1} max={totalPages} defaultValue={page + 1}
                       disabled={disabled} aria-label="Numéro de page"
                       onBlur={(e) => aller(e.currentTarget)}
                       onKeyDown={(e) => {
                           if (e.key === "Enter") aller(e.currentTarget);
                       }}/>
                / {totalPages}{info && <> {info}</>}
            </span>

            <button type="button" className="btn sm" disabled={disabled || derniere}
                    onClick={() => onChange(page + 1)} title="Page suivante" aria-label="Page suivante">Suivant</button>
            <button type="button" className="btn sm" disabled={disabled || derniere}
                    onClick={() => onChange(totalPages - 1)} title="Dernière page" aria-label="Dernière page">Dernier</button>
        </nav>
    );
}