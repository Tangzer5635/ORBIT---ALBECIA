import {useEffect, useState} from "react";
import Popup from "../Popup.tsx";
import {LIBELLE_ROLE, type Role, type Utilisateur, type UtilisateurRequest} from "../../../types/types.ts";
import {ajouterUtilisateur, modifierUtilisateur, supprimerUtilisateur, UtilisateurError,} from "../../../services/utilisateurService.ts";
import {getUtilisateur} from "../../../services/authService.ts";
const ROLES = Object.keys(LIBELLE_ROLE) as Role[];

export type PopupState =
    | { type: "creer" }
    | { type: "modifier"; utilisateur: Utilisateur }
    | { type: "supprimer"; utilisateur: Utilisateur }
    | null;

interface Props {
    popup: PopupState;
    onClose: () => void;
    onSuccess: () => void;
}

export default function ModifierUtilisateurPopup({popup, onClose, onSuccess}: Props) {
    const moi = getUtilisateur();
    const aModifier = popup?.type === "modifier" ? popup.utilisateur : undefined;
    const aSupprimer = popup?.type === "supprimer" ? popup.utilisateur : undefined;
    const estMoi = aModifier !== undefined && aModifier.id === moi?.id; // pas de changement de son propre rôle

    // champs du formulaire
    const [prenom, setPrenom] = useState("");
    const [nom, setNom] = useState("");
    const [login, setLogin] = useState("");
    const [nid, setNid] = useState("");
    const [role, setRole] = useState<Role>("STAGIAIRE");
    const [motDePasse, setMotDePasse] = useState("");
    const [erreur, setErreur] = useState("");
    const [envoi, setEnvoi] = useState(false);

    // à chaque ouverture / fermeture : champs vides en création, ceux de l'utilisateur en modification
    useEffect(() => {
        setPrenom(aModifier?.prenom ?? "");
        setNom(aModifier?.nom ?? "");
        setLogin(aModifier?.login ?? "");
        setNid("");
        setRole(aModifier?.role ?? "STAGIAIRE");
        setMotDePasse("");
        setErreur("");
    }, [popup]);

    function champ(set: (v: string) => void) {
        return (e: { target: { value: string } }) => {
            set(e.target.value);
            setErreur("");
        };
    }

    // Mêmes règles que l'entité Personnel côté back
    function verifier(creation: boolean): string {
        if (prenom.trim() === "" || prenom.trim().length > 30) return "Le prénom est obligatoire (30 caractères maximum).";
        if (nom.trim() === "" || nom.trim().length > 50) return "Le nom est obligatoire (50 caractères maximum).";
        if (!creation && (login.trim().length < 3 || login.trim().length > 50)) return "L’identifiant doit avoir entre 3 et 50 caractères.";
        if (creation && !/^[0-9]{10}$/.test(nid.trim())) return "Le NID doit contenir exactement 10 chiffres.";
        if (creation && motDePasse === "") return "Le mot de passe provisoire est obligatoire.";
        return "";
    }

    // Création et modification passent par la même fonction
    async function handleEnregistrer(aModifier?: Utilisateur) {
        const message = verifier(!aModifier);
        if (message) {
            setErreur(message);
            return;
        }

        const dto: UtilisateurRequest = {
            ...(aModifier ? {} : {nid: nid.trim()}),
            nom: nom.trim(),
            prenom: prenom.trim(),
            ...(aModifier ? {login: login.trim()} : {}), // en création, le back génère le login
            role,
            active: aModifier?.active ?? true,
            ...(motDePasse ? {motDePasse} : {}),
        };

        setEnvoi(true);
        try {
            if (aModifier) await modifierUtilisateur(aModifier.id, dto, aModifier);
            else await ajouterUtilisateur(dto);
            onClose();
            onSuccess();
        } catch (err) {
            if (err instanceof UtilisateurError && err.status === 403) setErreur("Vous n'avez pas le droit de gérer les utilisateurs.");
            else if (err instanceof UtilisateurError && err.status === 400) setErreur(err.detail || "Informations invalides.");
            else if (err instanceof UtilisateurError && err.status === 404) setErreur("Cet utilisateur n'a pas été créé.");
            else setErreur("Le service est indisponible. Réessayez plus tard.");
        } finally {
            setEnvoi(false);
        }
    }

    async function handleSupprimer(u: Utilisateur) {
        setEnvoi(true);
        try {
            await supprimerUtilisateur(u.id);
            onClose();
            onSuccess();
        } catch (err) {
            if (err instanceof UtilisateurError && err.status === 400) setErreur(err.detail || "Suppression impossible.");
            else if (err instanceof UtilisateurError && err.status === 403) setErreur("Vous n'avez pas le droit de supprimer un utilisateur.");
            else if (err instanceof UtilisateurError && err.status === 404) setErreur("Cet utilisateur n'existe plus.");
            // 500 : en pratique le compte est encore lié à un poste ou à des défaillances
            else if (err instanceof UtilisateurError && err.status === 500) setErreur("Ce compte occupe un poste ou a des défaillances à son nom. Désactivez-le plutôt.");
            else setErreur("Le service est indisponible. Réessayez plus tard.");
        } finally {
            setEnvoi(false);
        }
    }

    return (
        <Popup open={popup !== null} onClose={onClose} title={aSupprimer
                ? `Supprimer ${aSupprimer.prenom} ${aSupprimer.nom} ?`
                : aModifier ? "Modifier l’utilisateur" : "Créer un utilisateur"}
            description={aSupprimer
                ? `Le compte ${aSupprimer.login} sera supprimé définitivement. Pour bloquer l’accès sans supprimer, désactivez-le ou verrouillez-le.`
                : aModifier ? <>Identifiant actuel : <b>{aModifier.login}</b> (ID {aModifier.id})</> : "Donnez un mot de passe provisoire, à changer à la première connexion."}
            confirmLabel={aSupprimer ? "Supprimer l’utilisateur" : aModifier ? "Enregistrer" : "Créer l’utilisateur"}
            variant={aSupprimer ? "danger" : "primary"}
            onConfirm={aSupprimer ? () => handleSupprimer(aSupprimer) : () => handleEnregistrer(aModifier)}
            loading={envoi}
        >
            {!aSupprimer && (
                <>
                    <div className="user-grille">
                        <div>
                            <label htmlFor="uPrenom">Prénom</label>
                            <input id="uPrenom" value={prenom} onChange={champ(setPrenom)}/>
                        </div>
                        <div>
                            <label htmlFor="uNom">Nom</label>
                            <input id="uNom" value={nom} onChange={champ(setNom)}/>
                        </div>
                        {aModifier ? (
                            <div>
                                <label htmlFor="uLogin">Identifiant</label>
                                <input id="uLogin" value={login} onChange={champ(setLogin)} maxLength={50} autoComplete="off"/>
                            </div>
                        ) : (
                            <div>
                                <label htmlFor="uNid">NID</label>
                                <input id="uNid" value={nid} onChange={champ(setNid)} inputMode="numeric" maxLength={10} placeholder="10 chiffres"/>
                            </div>
                        )}
                        <div>
                            <label htmlFor="uRole">Rôle</label>
                            <select id="uRole" value={role} onChange={(e) => setRole(e.target.value as Role)} disabled={estMoi}>
                                {ROLES.map((r) => <option key={r} value={r}>{LIBELLE_ROLE[r]}</option>)}
                            </select>
                        </div>
                        <div>
                            <label htmlFor="uMdp">{aModifier ? "Nouveau mot de passe" : "Mot de passe provisoire"}</label>
                            <input id="uMdp" type="password" value={motDePasse} onChange={champ(setMotDePasse)} autoComplete="new-password" placeholder={aModifier ? "Laisser vide pour ne pas changer" : ""}/>
                        </div>
                    </div>
                    {estMoi &&
                        <p className="muted small">Vous ne pouvez pas changer votre propre rôle ni désactiver votre compte.</p>}
                </>
            )}
            {erreur && <p className="error-message" role="alert">{erreur}</p>}
        </Popup>
    );
}