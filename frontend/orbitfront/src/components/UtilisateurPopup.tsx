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

    // Mêmes règles que l'entité Personnel côté back
    function verifier(): string {
        if (prenom.trim() === "" || prenom.trim().length > 30) return "Le prénom est obligatoire (30 caractères maximum).";
        if (nom.trim() === "" || nom.trim().length > 50) return "Le nom est obligatoire (50 caractères maximum).";
        if (login.trim().length < 3 || login.trim().length > 50) return "L'identifiant doit faire entre 3 et 50 caractères.";
        if (creation && !/^[0-9]{10}$/.test(nid.trim())) return "Le NID doit contenir exactement 10 chiffres.";
        if (creation && motDePasse === "") return "Le mot de passe provisoire est obligatoire.";
        return "";
    }

    async function enregistrer() {
        const message = verifier();
        if (message) {
            setErreur(message);
            return;
        }

        const dto: UtilisateurRequest = {
            ...(creation ? {nid: nid.trim()} : {}), // en modification, le back garde le NID existant
            nom: nom.trim(),
            prenom: prenom.trim(),
            login: login.trim(),
            role,
            active: actif,
            ...(motDePasse ? {motDePasse} : {}), // vide = mot de passe inchangé
        };

        setEnvoi(true);
        try {
            const enregistre = utilisateur
                ? await modifierUtilisateur(utilisateur.id, dto)
                : await ajouterUtilisateur(dto);
            onEnregistre(enregistre);
        } catch (err) {
            if (err instanceof UtilisateurError && err.status === 403) setErreur("Vous n'avez pas le droit de gérer les utilisateurs.");
            else if (err instanceof UtilisateurError && err.status === 400) setErreur(err.detail || "Informations invalides.");
            else if (err instanceof UtilisateurError && err.status === 404) setErreur("Cet utilisateur n'existe plus.");
            else setErreur("Le service est indisponible. Réessayez plus tard.");
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
            onConfirm={enregistrer}
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