import { useCallback, useEffect, useMemo, useState } from "react";

const API = "http://localhost:8080/api/agent";
const HOSTS = [
    { id: "hote2", label: "HOTE 2", ip: "192.168.147.129" },
    { id: "hote3", label: "HOTE 3", ip: "192.168.147.130" },
] as const;

interface Proc {
    pid: number;
    name: string;
    username: string;
    cpu_percent: number;
    memory_percent: number;
}
interface AgentResponse {
    host: string;
    count: number;
    processes: Proc[];
}
interface HostProcess extends Proc {
    hostId: string;
    hostLabel: string;
}
interface Group {
    name: string;
    items: HostProcess[];
    cpu: number;
    memory: number;
}

export default function ProcessMonitor() {
    const [processes, setProcesses] = useState<HostProcess[]>([]);
    const [search, setSearch] = useState("");
    const [message, setMessage] = useState("");
    const [loading, setLoading] = useState(false);
    const [lastRefresh, setLastRefresh] = useState<Date | null>(null);
    const [status, setStatus] = useState<Record<string, string>>({});

    const load = useCallback(async () => {
        setLoading(true);
        const next: HostProcess[] = [];
        const nextStatus: Record<string, string> = {};
        await Promise.all(HOSTS.map(async (host) => {
            try {
                const response = await fetch(`${API}/processes?host=${host.id}`);
                if (!response.ok) throw new Error(`HTTP ${response.status}`);
                const data = await response.json() as AgentResponse;
                for (const process of data.processes ?? []) {
                    next.push({ ...process, hostId: host.id, hostLabel: host.label });
                }
                nextStatus[host.id] = `Connecté · ${data.count} fenêtres/processus`;
            } catch (e) {
                nextStatus[host.id] = `Hors ligne (${e instanceof Error ? e.message : "erreur"})`;
            }
        }));
        setProcesses(next);
        setStatus(nextStatus);
        setLastRefresh(new Date());
        setLoading(false);
    }, []);

    useEffect(() => {
        void load();
        const timer = window.setInterval(() => void load(), 5000);
        return () => window.clearInterval(timer);
    }, [load]);

    const groups = useMemo(() => {
        const byName = new Map<string, Group>();
        for (const process of processes) {
            const name = process.name || "(inconnu)";
            const group = byName.get(name) ?? { name, items: [], cpu: 0, memory: 0 };
            group.items.push(process);
            group.cpu += process.cpu_percent || 0;
            group.memory += process.memory_percent || 0;
            byName.set(name, group);
        }
        const q = search.trim().toLowerCase();
        return [...byName.values()]
            .filter((g) => g.name.toLowerCase().includes(q))
            .sort((a, b) => b.memory - a.memory);
    }, [processes, search]);

    async function killGroup(group: Group) {
        const byHost = group.items.reduce<Record<string, HostProcess[]>>((acc, item) => {
            (acc[item.hostId] ??= []).push(item);
            return acc;
        }, {});
        const details = Object.entries(byHost).map(([hostId, items]) =>
            `${HOSTS.find(h => h.id === hostId)?.label}: ${items.length} PID(s) ${items.map(p => p.pid).join(", ")}`
        ).join("\n");
        if (!window.confirm(`Arrêter toutes les instances visibles de « ${group.name} » ?\n\n${details}\n\nCette action fermera ces applications sur les VM concernées.`)) return;

        const errors: string[] = [];
        for (const item of group.items) {
            try {
                const response = await fetch(`${API}/kill/${item.pid}?host=${item.hostId}`, { method: "POST" });
                if (!response.ok) errors.push(`${item.hostLabel} PID ${item.pid}: HTTP ${response.status}`);
            } catch {
                errors.push(`${item.hostLabel} PID ${item.pid}: agent injoignable`);
            }
        }
        setMessage(errors.length ? `Certaines instances n'ont pas pu être arrêtées : ${errors.join(" ; ")}` : `Toutes les instances visibles de ${group.name} ont été arrêtées.`);
        await load();
    }

    return (
        <section className="card-s" style={{ marginTop: 14 }}>
            <div className="page-head">
                <h1>Processus actifs des VM</h1>
                <span className="muted">{groups.length} noms · {processes.length} instances visibles</span>
            </div>
            <p className="muted small">
                Inventaire en direct des fenêtres visibles sur HOTE 2 et HOTE 3. Les processus portant le même nom
                sont regroupés. « Arrêter » agit sur toutes les instances du groupe dans les deux VM.
                Les processus sans fenêtre visible ne sont pas affichés et l'agent refuse de les arrêter.
            </p>

            <div style={{ display: "grid", gridTemplateColumns: "repeat(auto-fit, minmax(230px, 1fr))", gap: 10, margin: "12px 0" }}>
                {HOSTS.map(host => (
                    <div key={host.id} style={{ border: "1px solid var(--border, #555)", borderRadius: 8, padding: 12 }}>
                        <strong>{host.label}</strong> <span className="muted small">· {host.ip}</span>
                        <div style={{ marginTop: 5, color: status[host.id]?.startsWith("Connecté") ? "var(--success, #27975b)" : "var(--danger, #d44)" }}>
                            {status[host.id] ?? "Connexion en cours…"}
                        </div>
                    </div>
                ))}
            </div>

            <div style={{ display: "flex", flexWrap: "wrap", gap: 8, alignItems: "center", margin: "12px 0" }}>
                <input className="search" style={{ flex: "1 1 240px" }} type="search"
                    placeholder="Rechercher un nom de processus…" value={search}
                    onChange={e => setSearch(e.target.value)} />
                <button type="button" className="btn primary" onClick={() => void load()} disabled={loading}>
                    {loading ? "Actualisation…" : "Rafraîchir"}
                </button>
                {lastRefresh && <span className="muted small">Mis à jour à {lastRefresh.toLocaleTimeString()}</span>}
            </div>
            {message && <p role="status" className="muted">{message}</p>}

            <div style={{ overflowX: "auto" }}>
                <table className="tbl">
                    <thead>
                        <tr>
                            <th>Processus</th>
                            <th>HOTE 2</th>
                            <th>HOTE 3</th>
                            <th className="n">CPU cumulé %</th>
                            <th className="n">Mémoire cumulée %</th>
                            <th></th>
                        </tr>
                    </thead>
                    <tbody>
                        {groups.map(group => {
                            const a = group.items.filter(p => p.hostId === "hote2");
                            const b = group.items.filter(p => p.hostId === "hote3");
                            return (
                                <tr key={group.name}>
                                    <td className="mono">{group.name}</td>
                                    <td title={`PID : ${a.map(p => p.pid).join(", ") || "aucun"}`}>{a.length} instance(s)</td>
                                    <td title={`PID : ${b.map(p => p.pid).join(", ") || "aucun"}`}>{b.length} instance(s)</td>
                                    <td className="n">{group.cpu.toFixed(1)}</td>
                                    <td className="n">{group.memory.toFixed(1)}</td>
                                    <td className="n">
                                        <button type="button" className="btn sm danger" onClick={() => void killGroup(group)}>
                                            Arrêter
                                        </button>
                                    </td>
                                </tr>
                            );
                        })}
                        {groups.length === 0 && (
                            <tr><td colSpan={6} className="muted">
                                {loading ? "Chargement des processus…" : "Aucun processus avec une fenêtre visible (ou les deux agents sont injoignables)."}
                            </td></tr>
                        )}
                    </tbody>
                </table>
            </div>
        </section>
    );
}
