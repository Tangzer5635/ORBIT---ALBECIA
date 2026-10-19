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
import type { Batiment } from "../types/types.ts";
import { getBatiments } from "../services/batimentService.ts";

interface BatimentsContextValue {
    batiments: Batiment[];
    setBatiments: Dispatch<SetStateAction<Batiment[]>>;
    erreurChargement: string | undefined;
}

const BatimentsContext = createContext<BatimentsContextValue | null>(null);

export function BatimentsProvider({ children }: { children: ReactNode }) {
    const [liste, setBatiments] = useState<Batiment[]>([]);
    const [erreurChargement, setErreurChargement] = useState<string>();

    const batiments = useMemo(
        () => [...liste].sort((a, b) => a.numBatiment.localeCompare(b.numBatiment, "fr", { numeric: true })),
        [liste]
    );

    useEffect(() => {
        getBatiments()
            .then(setBatiments)
            .catch(() => setErreurChargement("Impossible de charger les bâtiments."));
    }, []);

    return (
        <BatimentsContext.Provider value={{ batiments, setBatiments, erreurChargement }}>
            {children}
        </BatimentsContext.Provider>
    );
}

export function useBatiments() {
    const ctx = useContext(BatimentsContext);
    if (!ctx) throw new Error("useBatiments doit être utilisé dans un BatimentsProvider");
    return ctx;
}