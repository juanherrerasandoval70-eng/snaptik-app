param (
    [Parameter(Mandatory=$true)]
    [string]$Token
)

$Token = $Token.Trim()
if ([string]::IsNullOrWhiteSpace($Token)) {
    Write-Host "[ERROR] El token no puede estar vacio." -ForegroundColor Red
    exit 1
}

Write-Host "Conectando y subiendo archivos a https://github.com/juanherrerasandoval70-eng/snaptik-app..." -ForegroundColor Cyan

$remoteWithAuth = "https://${Token}@github.com/juanherrerasandoval70-eng/snaptik-app.git"
$remoteClean = "https://github.com/juanherrerasandoval70-eng/snaptik-app.git"

git remote set-url origin $remoteWithAuth
$env:GIT_TERMINAL_PROMPT = "0"

git push -u origin main --force

# Restaurar URL limpia para no dejar el token expuesto en git config
git remote set-url origin $remoteClean

Write-Host "`n[EXITO] Repositorio sincronizado exitosamente con GitHub!" -ForegroundColor Green
