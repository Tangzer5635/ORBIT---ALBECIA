# Installe l'Agent ORBIT en tâche planifiée : il démarre tout seul à chaque ouverture de session Windows.
# À lancer UNE fois sur chaque VM, dans un PowerShell administrateur, depuis le dossier qui contient
# orbit_agent_windows.py :
#     powershell -ExecutionPolicy Bypass -File .\installer_agent_windows.ps1
#
# « À l'ouverture de session » et pas « au démarrage » : au démarrage, la tâche tournerait hors de la
# session de l'utilisateur et l'agent ne verrait aucune fenêtre (liste de processus vide).

$ErrorActionPreference = "Stop"
$nomTache = "ORBIT Agent"
$script = Join-Path $PSScriptRoot "orbit_agent_windows.py"
if (-not (Test-Path $script)) { throw "orbit_agent_windows.py introuvable dans $PSScriptRoot" }

# pythonw : Python sans fenêtre de console (les messages vont dans orbit_agent.log)
$pythonw = (Get-Command pythonw.exe -ErrorAction SilentlyContinue).Source
if (-not $pythonw) { $pythonw = (Get-Command pyw.exe -ErrorAction SilentlyContinue).Source }
if (-not $pythonw) { throw "pythonw.exe / pyw.exe introuvable : installer Python 3 (avec le lanceur py)" }

$utilisateur = "$env:USERDOMAIN\$env:USERNAME"
$action    = New-ScheduledTaskAction -Execute $pythonw -Argument "`"$script`"" -WorkingDirectory $PSScriptRoot
$trigger   = New-ScheduledTaskTrigger -AtLogOn -User $utilisateur
# Highest : l'agent peut aussi arrêter les applications lancées en administrateur
$principal = New-ScheduledTaskPrincipal -UserId $utilisateur -LogonType Interactive -RunLevel Highest
# pas de limite de durée, et redémarrage automatique si l'agent plante
$reglages  = New-ScheduledTaskSettingsSet -AllowStartIfOnBatteries -DontStopIfGoingOnBatteries `
             -ExecutionTimeLimit ([TimeSpan]::Zero) -RestartCount 3 -RestartInterval (New-TimeSpan -Minutes 1)

Register-ScheduledTask -TaskName $nomTache -Action $action -Trigger $trigger -Principal $principal `
                       -Settings $reglages -Force | Out-Null
Start-ScheduledTask -TaskName $nomTache
Write-Host "Tache '$nomTache' installee pour $utilisateur et demarree. Journal : $(Join-Path $PSScriptRoot 'orbit_agent.log')"