# Installe et lance l'Agent ORBIT sur ce PC, sans droits administrateur.
# A mettre dans le meme dossier que orbit_agent_windows.py (et, si Python n'est pas installe,
# l'installeur python-3.x-amd64.exe). Lancer avec installer_orbit.bat (double-clic).

$ErrorActionPreference = "Stop"

# ---- a modifier si besoin ----
$Serveur = "ws://172.16.64.192:8080"
$Token   = "xxx"
# ------------------------------

$Dossier = $PSScriptRoot
$Agent   = Join-Path $Dossier "orbit_agent_windows.py"
if (-not (Test-Path $Agent)) { throw "orbit_agent_windows.py introuvable dans $Dossier" }

# Cherche un vrai Python 3 (on ignore le faux python.exe du Microsoft Store)
function Trouver-Python {
    foreach ($nom in "py.exe", "python.exe") {
        $cmd = Get-Command $nom -ErrorAction SilentlyContinue |
               Where-Object { $_.Source -notlike "*\WindowsApps\*" } | Select-Object -First 1
        if ($cmd) {
            $v = & $cmd.Source --version 2>&1
            if ($LASTEXITCODE -eq 0 -and "$v" -match "^Python 3") { return $cmd.Source }
        }
    }
    # Python installe pour l'utilisateur mais pas encore dans le PATH de cette fenetre
    $c = Get-ChildItem "$env:LOCALAPPDATA\Programs\Python\Python3*\python.exe" -ErrorAction SilentlyContinue |
         Select-Object -First 1
    if ($c) { return $c.FullName }
    return $null
}

# 1. Python
$python = Trouver-Python
if (-not $python) {
    $installeur = Get-ChildItem $Dossier -Filter "python-3*.exe" -ErrorAction SilentlyContinue | Select-Object -First 1
    if (-not $installeur) {
        throw "Python 3 n'est pas installe. Mets l'installeur (python-3.x-amd64.exe) dans $Dossier puis relance."
    }
    Write-Host "Installation de Python pour cet utilisateur (sans admin), 1 a 2 minutes..."
    Start-Process $installeur.FullName -Wait -ArgumentList "/quiet", "InstallAllUsers=0", "PrependPath=1", "Include_launcher=0", "Include_test=0"
    $python = Trouver-Python
    if (-not $python) { throw "L'installation de Python a echoue." }
}
Write-Host "Python : $python"

# pythonw / pyw = Python sans fenetre de console (les messages vont dans orbit_agent.log)
$nomW = if ((Split-Path $python -Leaf) -eq "py.exe") { "pyw.exe" } else { "pythonw.exe" }
$pythonw = Join-Path (Split-Path $python) $nomW

# 2. Dependances
Write-Host "Installation des dependances..."
& $python -m pip install --user --quiet psutil websocket-client
if ($LASTEXITCODE -ne 0) { throw "pip a echoue (reseau ? proxy ?)" }

# 3. Configuration de l'agent (variables de l'utilisateur, valables aussi pour cette session)
[Environment]::SetEnvironmentVariable("ORBIT_SERVEUR", $Serveur, "User")
[Environment]::SetEnvironmentVariable("ORBIT_AGENT_TOKEN", $Token, "User")
$env:ORBIT_SERVEUR = $Serveur
$env:ORBIT_AGENT_TOKEN = $Token

# 4. Arrete une ancienne instance de l'agent si elle tourne
Get-CimInstance Win32_Process |
    Where-Object { $_.CommandLine -like "*orbit_agent_windows.py*" -and $_.ProcessId -ne $PID } |
    ForEach-Object { Stop-Process -Id $_.ProcessId -Force -ErrorAction SilentlyContinue }

# 5. Lancement automatique a l'ouverture de session (dossier Demarrage, sans admin)
$raccourci = Join-Path ([Environment]::GetFolderPath("Startup")) "ORBIT Agent.lnk"
$s = (New-Object -ComObject WScript.Shell).CreateShortcut($raccourci)
$s.TargetPath = $pythonw
$s.Arguments = "`"$Agent`""
$s.WorkingDirectory = $Dossier
$s.Save()

# 6. Lancement maintenant
Start-Process $pythonw -ArgumentList "`"$Agent`"" -WorkingDirectory $Dossier
Write-Host "Agent lance. Verification de la connexion..."
Start-Sleep -Seconds 6
$log = Join-Path $Dossier "orbit_agent.log"
if (Test-Path $log) { Get-Content $log -Tail 6 } else { Write-Host "Pas encore de journal : regarde $log dans quelques secondes." }
