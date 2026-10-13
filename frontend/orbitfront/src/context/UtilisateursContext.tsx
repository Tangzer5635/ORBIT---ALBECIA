import {
    createContext,
    useContext,
    useEffect,
    useState,
    type Dispatch,
    type ReactNode,
    type SetStateAction,
    useMemo
} from "react";
import type { Utilisateur } from "../types/types.ts";
import { getUtilisateur } from "../services/user.ts";

interface UsersContextValue {
    users: Utilisateur[];
    setUsers: Dispatch<SetStateAction<Utilisateur[]>>;
    erreurChargement: string | undefined;
}

const UsersContext = createContext<UsersContextValue | null>(null);

export function UsersProvider({ children }: { children: ReactNode }) {
    const [liste, setUsers] = useState<Utilisateur[]>([]);
    const [erreurChargement, setErreurChargement] = useState<string>();

    // Tri par nom puis prénom (id est un nombre : pour trier par id, (a, b) => a.id - b.id)
    const users = useMemo(
        () => [...liste].sort((a, b) =>
            a.nom.localeCompare(b.nom, "fr") || a.prenom.localeCompare(b.prenom, "fr")),
        [liste]
    );

    useEffect(() => {
        getUtilisateur()
            .then(setUsers)
            .catch(() => setErreurChargement("Impossible de charger les utilisateurs."));
    }, []);

    return (
        <UsersContext.Provider value={{ users, setUsers, erreurChargement }}>
            {children}
        </UsersContext.Provider>
    );
}

export function useUsers() {
    const ctx = useContext(UsersContext);
    if (!ctx) throw new Error("useUsers doit être utilisé dans un UsersProvider");
    return ctx;
}