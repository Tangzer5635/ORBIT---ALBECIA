import {LIBELLE_ROLE, type Utilisateur} from "../../types/types.ts";

interface UtilisateurLigneProps {
    utilisateur: Utilisateur;
    estMoi: boolean;
    onModifier: (u: Utilisateur) => void;
    onVerrou: (u: Utilisateur) => void;
    onActif: (u: Utilisateur) => void;
    onSupprime: (u: Utilisateur) => void;
}

// Une ligne de la liste des utilisateurs (liseré rouge si le compte est verrouillé)
export default function UtilisateurLigne({
                                             utilisateur: u,
                                             estMoi,
                                             onModifier,
                                             onVerrou,
                                             onActif,
                                             onSupprime
                                         }: UtilisateurLigneProps) {
    return (
        <div className={`li ${u.verrouille ? "def" : "dispo"}`}>
            <strong>
                {u.prenom} {u.nom}
                {estMoi && <span className="muted"> (vous)</span>}
            </strong>
            <strong>{LIBELLE_ROLE[u.role] ?? u.role}</strong>

            <span>
                ID {u.id}, {u.login}, {u.active ? "actif" : <b>désactivé</b>}
                {u.verrouille && <>, <b>verrouillé</b></>}
            </span>

            {estMoi ? (
                <span/>
            ) : (
                <span className="li-actions">
                    <button type="button" className="btn sm" onClick={() => onModifier(u)}>Modifier</button>
                    {/* le back refuse de verrouiller un administrateur */}
                    {u.role !== "ADMINISTRATEUR" && (
                        <>
                            <button type="button" className="btn sm" onClick={() => onActif(u)}>
                                {u.active ? "Désactiver" : "Activer"}
                            </button>
                            <button type="button" className="btn sm" onClick={() => onVerrou(u)}>
                                {u.verrouille ? "Déverrouiller" : "Verrouiller"}
                            </button>
                        </>
                    )}
                    <button type="button" className="btn sm danger" onClick={() => onSupprime(u)}>Supprimer</button>                </span>
            )}
        </div>
    );
}
