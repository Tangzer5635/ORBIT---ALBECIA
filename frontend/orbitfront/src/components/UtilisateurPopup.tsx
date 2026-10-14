import {useState} from "react";
import Popup from "./Popup.tsx";
import {LIBELLE_ROLE, type Role, type Utilisateur, type UtilisateurRequest} from "../types/types.ts";
import {ajouterUtilisateur, modifierUtilisateur, UtilisateurError} from "../services/utilisateurService.ts";

const ROLES = Object.keys(LIBELLE_ROLE) as Role[];

interface Props {
    utilisateur?: Utilisateur;   // absent = création, présent = modification
    estMoi: boolean;             // l'admin connecté ne peut pas changer son propre rôle ni se désactiver
    onClose: () => void;
    onEnregistre: (utilisateur: Utilisateur) => void; // reçoit l'utilisateur renvoyé par le back
}

// Popup de création / modification d'un utilisateur (même formulaire pour les deux)
export default function UtilisateurPopup({utilisateur, estMoi, onClose, onEnregistre}: Props) {
    // champs pré-remplis en modification, vides en création
    const [prenom, setPrenom] = useState(utilisateur?.prenom ?? "");
    const [nom, setNom] = useState(utilisateur?.nom ?? "");
    const [login, setLogin] = useState(utilisateur?.login ?? "");
    const [nid, setNid] = useState("");
    const [role, setRole] = useState<Role>(utilisateur?.role ?? "STAGIAIRE");
    const [motDePasse, setMotDePasse] = useState("");
    const [actif, setActif] = useState(utilisateur?.active ?? true);
    const [erreur, setErreur] = useState("");
    const [envoi, setEnvoi] = useState(false);

    const creation = utilisateur === undefined;

    // regle commune à la création et modification d'un utilisateur
    function verifierChamps(): string {
        if (prenom.trim() === "" || prenom.trim().length > 30) return "Le prénom est obligatoire (30 caractères maximum).";
        if (nom.trim() === "" || nom.trim().length > 50) return "Le nom est obligatoire (50 caractères maximum).";
        if (login.trim().length < 3 || login.trim().length > 50) return "L'identifiant doit faire entre 3 et 50 caractères.";
        return "";
    }

    function messageErreur(err: unknown, message404: string): string {
        if (err instanceof UtilisateurError) {
            if (err.status === 403) return "Vous n'avez pas le droit de gérer les utilisateurs.";
            if (err.status === 400) return err.detail || "Informations invalides.";
            if (err.status === 404) return message404;
        }
        return "Le service est indisponible. Réessayez plus tard.";
    }

    // NID et mot de passe provisoire obligatoires
    async function ajouter() {
        const message = verifierChamps()
            || (!/^[0-9]{10}$/.test(nid.trim()) ? "Le NID doit contenir exactement 10 chiffres." : "")
            || (motDePasse === "" ? "Le mot de passe provisoire est obligatoire." : "");
        if (message) {
            setErreur(message);
            return;
        }

        const dto: UtilisateurRequest = {
            nid: nid.trim(),
            nom: nom.trim(),
            prenom: prenom.trim(),
            login: login.trim(),
            role,
            active: actif,
            motDePasse,
        };

        setEnvoi(true);
        try {
            onEnregistre(await ajouterUtilisateur(dto));
        } catch (err) {
            setErreur(messageErreur(err, "La création a échoué : service introuvable."));
        } finally {
            setEnvoi(false);
        }
    }

    // Modification : le back garde le NID existant, mot de passe vide = inchangé
    async function modifier(aModifier: Utilisateur) {
        const message = verifierChamps();
        if (message) {
            setErreur(message);
            return;
        }

        const dto: UtilisateurRequest = {
            nom: nom.trim(),
            prenom: prenom.trim(),
            login: login.trim(),
            role,
            active: actif,
            ...(motDePasse ? {motDePasse} : {}),
        };

        setEnvoi(true);
        try {
            onEnregistre(await modifierUtilisateur(aModifier.id, dto));
        } catch (err) {
            setErreur(messageErreur(err, "Cet utilisateur n'existe plus."));
        } finally {
            setEnvoi(false);
        }
    }

    // efface l'erreur dès qu'on retape quelque chose
    function champ(set: (v: string) => void) {
        return (e: { target: { value: string } }) => {
            set(e.target.value);
            setErreur("");
        };
    }

    return (
        <Popup
            open
            onClose={onClose}
            title={creation ? "Créer un utilisateur" : "Modifier l’utilisateur"}
            description={utilisateur
                ? <>Identifiant actuel : <b>{utilisateur.login}</b> (ID {utilisateur.id})</>
                : "Donnez un mot de passe provisoire, à changer à la première connexion."}
            confirmLabel={creation ? "Créer l’utilisateur" : "Enregistrer"}
            onConfirm={utilisateur ? () => modifier(utilisateur) : ajouter}
            loading={envoi}
        >
            <div className="user-grille">
                <div>
                    <label htmlFor="uPrenom">Prénom</label>
                    <input id="uPrenom" value={prenom} onChange={champ(setPrenom)} autoFocus/>
                </div>
                <div>
                    <label htmlFor="uNom">Nom</label>
                    <input id="uNom" value={nom} onChange={champ(setNom)}/>
                </div>
                <div>
                    <label htmlFor="uLogin">Identifiant</label>
                    <input id="uLogin" value={login} onChange={champ(setLogin)} autoComplete="off"/>
                </div>
                {creation && (
                    <div>
                        <label htmlFor="uNid">NID</label>
                        <input id="uNid" value={nid} onChange={champ(setNid)} inputMode="numeric"
                               maxLength={10} placeholder="10 chiffres"/>
                    </div>
                )}
                <div>
                    <label htmlFor="uRole">Rôle</label>
                    <select id="uRole" value={role} onChange={(e) => setRole(e.target.value as Role)} disabled={estMoi}>
                        {ROLES.map((r) => <option key={r} value={r}>{LIBELLE_ROLE[r]}</option>)}
                    </select>
                </div>
                <div>
                    <label htmlFor="uMdp">{creation ? "Mot de passe provisoire" : "Nouveau mot de passe"}</label>
                    <input
                        id="uMdp"
                        type="password"
                        value={motDePasse}
                        onChange={champ(setMotDePasse)}
                        autoComplete="new-password"
                        placeholder={creation ? "" : "Laisser vide pour ne pas changer"}
                    />
                </div>
            </div>

            {!creation && (
                <label className="user-check">
                    <input type="checkbox" checked={actif} onChange={(e) => setActif(e.target.checked)} disabled={estMoi}/>
                    <span>Compte actif. Décoché, l’utilisateur ne peut plus se connecter mais son historique est conservé.</span>
                </label>
            )}
            {estMoi && <p className="muted small">Vous ne pouvez pas changer votre propre rôle ni désactiver votre compte.</p>}

            {erreur && <p className="error-message" role="alert">{erreur}</p>}
        </Popup>
    );
}