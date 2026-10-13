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
import type { Salle } from "../types/types.ts";
import {getSalle} from "../services/salleService.ts";

interface SallesContextValue {
    salles: Salle[];
    setSalles: Dispatch<SetStateAction<Salle[]>>;
    erreurChargement: string | undefined;
}

const SalleContext = createContext<SallesContextValue | null>(null);

export function SalleProvider({ children }: { children: ReactNode }) {
    const [liste, setSalles] = useState<Salle[]>([]);
    const [erreurChargement, setErreurChargement] = useState<string>();

    const salles = useMemo(
        () => [...liste].sort((a, b) => a.numSalle.localeCompare(b.numSalle, "fr", { numeric: true })),
        [liste]
    );

    useEffect(() => {
        getSalle()
            .then(setSalles)
            .catch(() => setErreurChargement("Impossible de charger les bâtiments."));
    }, []);

    return (
        <SalleContext.Provider value={{ salles, setSalles, erreurChargement }}>
            {children}
        </SalleContext.Provider>
    );
}

export function useSalles() {
    const ctx = useContext(SalleContext);
    if (!ctx) throw new Error("useBatiments doit être utilisé dans un BatimentsProvider");
    return ctx;
}