import { useEffect, useState } from "react";

// En dev : le back Spring tourne sur 8080. Adapte si besoin (ou passe par un proxy Vite).
const API = "http://localhost:8080/api/agent";

interface Proc {
  pid: number;
  name: string;
  username: string;
  cpu_percent: number;
  memory_percent: number;
}

interface Group {
  name: string;
  count: number;
  cpu: number;
  mem: number;
  pids: number[];
  isApp: boolean; // true = appli utilisateur (tuable), false = process système
}

type Filter = "app" | "system" | "all";

// Un process appartient à l'utilisateur si son propriétaire n'est pas un compte système Windows.
function isUserProcess(username: string): boolean {
  if (!username) return false;
  const u = username.toUpperCase();
  if (u.includes("NT AUTHORITY") || u.includes("AUTORITE NT")) return false;
  return true;
}

export default function ProcessMonitor() {
  const [host, setHost] = useState("");
  const [procs, setProcs] = useState<Proc[]>([]);
  const [search, setSearch] = useState("");
  const [filter, setFilter] = useState<Filter>("app"); // par défaut : applications seulement
  const [msg, setMsg] = useState("");

  async function load() {
    try {
      const r = await fetch(`${API}/processes`);
      if (!r.ok) {
        setMsg(`Erreur ${r.status}`);
        return;
      }
      const j = await r.json();
      setHost(`${j.host} — ${j.count} process`);
      setProcs(j.processes);
      setMsg("");
    } catch {
      setMsg("Agent injoignable");
    }
  }

  async function killGroup(g: Group) {
    if (!g.isApp) return; // sécurité : on ne tue jamais un process système
    const label = g.count > 1 ? `${g.name} (${g.count} process)` : g.name;
    if (!confirm(`Tuer ${label} ?`)) return;
    for (const pid of g.pids) {
      await fetch(`${API}/kill/${pid}`, { method: "POST" });
    }
    setMsg(`Tué : ${label}`);
    load();
  }

  useEffect(() => {
    load();
    const t = setInterval(load, 5000); // rafraîchit toutes les 5 s
    return () => clearInterval(t);
  }, []);

  // regroupe les process par nom
  const groups: Record<string, Group> = {};
  for (const p of procs) {
    const name = p.name || "(inconnu)";
    if (!groups[name])
      groups[name] = { name, count: 0, cpu: 0, mem: 0, pids: [], isApp: false };
    const g = groups[name];
    g.count++;
    g.cpu += p.cpu_percent ?? 0;
    g.mem += p.memory_percent ?? 0;
    g.pids.push(p.pid);
    if (isUserProcess(p.username)) g.isApp = true; // au moins un process utilisateur → groupe appli
  }

  const rows = Object.values(groups)
      .filter((g) => g.name.toLowerCase().includes(search.toLowerCase()))
      .filter((g) => filter === "all" || (filter === "app" ? g.isApp : !g.isApp))
      .sort((a, b) => b.mem - a.mem);

  const tab = (key: Filter, label: string) => (
      <button
          onClick={() => setFilter(key)}
          style={{
            border: 0,
            borderRadius: 4,
            cursor: "pointer",
            padding: ".35rem .7rem",
            background: filter === key ? "#2c3e50" : "#ddd",
            color: filter === key ? "#fff" : "#333",
          }}
      >
        {label}
      </button>
  );

  return (
      <div style={{ padding: "1rem", fontFamily: "system-ui, sans-serif" }}>
        <h2>Processus — {host}</h2>

        <div style={{ display: "flex", gap: ".5rem", margin: ".8rem 0", alignItems: "center", flexWrap: "wrap" }}>
          {tab("app", "Applications")}
          {tab("system", "Système")}
          {tab("all", "Tous")}
          <input
              placeholder="Filtrer par nom..."
              value={search}
              onChange={(e) => setSearch(e.target.value)}
          />
          <button onClick={load}>Rafraîchir</button>
          <span style={{ color: "#666" }}>{msg}</span>
        </div>

        <table style={{ width: "100%", borderCollapse: "collapse", fontSize: ".9rem" }}>
          <thead>
          <tr style={{ textAlign: "left", borderBottom: "1px solid #ccc" }}>
            <th>Nom</th>
            <th style={{ textAlign: "right" }}>Inst.</th>
            <th style={{ textAlign: "right" }}>CPU %</th>
            <th style={{ textAlign: "right" }}>Mém %</th>
            <th></th>
          </tr>
          </thead>
          <tbody>
          {rows.map((g) => (
              <tr key={g.name} style={{ borderBottom: "1px solid #eee" }}>
                <td>{g.name}</td>
                <td style={{ textAlign: "right" }} title={`PID : ${g.pids.join(", ")}`}>
                  {g.count}
                </td>
                <td style={{ textAlign: "right" }}>{g.cpu.toFixed(1)}</td>
                <td style={{ textAlign: "right" }}>{g.mem.toFixed(1)}</td>
                <td>
                  <button
                      disabled={!g.isApp}
                      title={g.isApp ? "" : "Process système — protégé"}
                      style={{
                        background: g.isApp ? "#c0392b" : "#bbb",
                        color: "#fff",
                        border: 0,
                        borderRadius: 4,
                        cursor: g.isApp ? "pointer" : "not-allowed",
                        padding: ".3rem .6rem",
                      }}
                      onClick={() => killGroup(g)}
                  >
                    Tuer
                  </button>
                </td>
              </tr>
          ))}
          </tbody>
        </table>
      </div>
  );
}