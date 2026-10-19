import { useEffect, type ReactNode, type FormEvent } from "react";

type Props = {
    open: boolean;
    onClose: () => void;
    title: string;
    description?: ReactNode;
    children?: ReactNode;
    confirmLabel: string;
    onConfirm: () => void;
    variant?: "primary" | "danger";
    loading?: boolean;
    disabled?: boolean;
    cancelLabel?: string;
    onCancel?: () => void;
};

export default function Popup({
                                  open, onClose, title, description, children,
                                  confirmLabel, onConfirm, variant = "primary", loading, disabled, cancelLabel, onCancel,
                              }: Props) {
    // Échap pour fermer + bloque le scroll de la page
    useEffect(() => {
        if (!open) return;
        const onKey = (e: KeyboardEvent) => e.key === "Escape" && onClose();
        document.addEventListener("keydown", onKey);
        document.body.style.overflow = "hidden";
        return () => {
            document.removeEventListener("keydown", onKey);
            document.body.style.overflow = "";
        };
    }, [open, onClose]);

    if (!open) return null;

    const submit = (e: FormEvent) => {
        e.preventDefault();
        onConfirm();
    };

    return (
        <div className="popup-overlay" onMouseDown={onClose}>
            <form
                className="popup"
                role="dialog"
                aria-modal="true"
                onMouseDown={(e) => e.stopPropagation()}
                onSubmit={submit}
            >
                <div className="popup-handle" />
                <header className="popup-header">
                    <h2>{title}</h2>
                    <button type="button" className="popup-close" onClick={onClose} aria-label="Fermer">×</button>
                </header>

                {description && <p className="popup-desc">{description}</p>}
                <div className="popup-body">{children}</div>

                <footer className="popup-footer">
                    <button type="button" className="btn" onClick={onCancel ?? onClose}>{cancelLabel ?? "Annuler"}</button>
                    <button type="submit" className={`btn ${variant}`} disabled={disabled || loading}>
                        {loading ? "…" : confirmLabel}
                    </button>
                </footer>
            </form>
        </div>
    );
}