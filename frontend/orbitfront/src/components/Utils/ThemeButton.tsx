import { useEffect, useState } from "react";
import { Moon, Sun } from "lucide-react";

export default function ThemeButton() {
    const [clair, setClair] = useState(localStorage.getItem("orbit-theme") === "light");

    // À chaque changement : on applique le thème et on l'enregistre dans le navigateur
    useEffect(() => {
        const theme = clair ? "light" : "dark";
        document.documentElement.dataset.theme = theme;
        localStorage.setItem("orbit-theme", theme);
    }, [clair]);

    return (
        <button type="button" className="theme-button" onClick={() => setClair(!clair)}>
            {clair ? (
                <Moon size={20} color="black" strokeWidth={1.5} />
            ) : (
                <Sun size={20} color="white" strokeWidth={1.5} />
            )}
        </button>
    );
}