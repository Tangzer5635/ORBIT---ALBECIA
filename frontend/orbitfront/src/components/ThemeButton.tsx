import { useEffect, useState } from "react";


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
            {clair ? "🌙" : "☀️"}
        </button>
    );
}