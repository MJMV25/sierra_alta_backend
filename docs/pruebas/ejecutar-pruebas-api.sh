#!/usr/bin/env bash
# Ejecuta contra el backend en marcha los casos del catálogo que se verifican por la API.
# Uso:  bash docs/pruebas/ejecutar-pruebas-api.sh [URL_BASE]     (por defecto http://localhost:$SERVER_PORT)
# Requiere el perfil dev (cuentas de demostración) y lee las contraseñas de .env sin mostrarlas.
set -u

ENV_FILE="$(dirname "$0")/../../.env"
leer_env() { grep -E "^$1=" "$ENV_FILE" | head -1 | cut -d= -f2- | tr -d '\r'; }

PUERTO="$(leer_env SERVER_PORT)"
BASE="${1:-http://localhost:${PUERTO:-8080}}"
API="$BASE/api/v1"
CLAVE_DEMO="$(leer_env DEMO_USERS_PASSWORD)"
ADMIN="$(leer_env BOOTSTRAP_ADMIN_USERNAME)"
CLAVE_ADMIN="$(leer_env BOOTSTRAP_ADMIN_PASSWORD)"
TMP="$(mktemp -d)"
trap 'rm -rf "$TMP"' EXIT

aprobados=0
fallidos=0
bloqueados=0

# verificar <caso> <descripción> <esperado> <obtenido>
verificar() {
	if [ "$3" = "$4" ]; then
		aprobados=$((aprobados + 1)); printf 'APROBADO  %-7s %s (esperado %s)\n' "$1" "$2" "$3"
	else
		fallidos=$((fallidos + 1)); printf 'FALLIDO   %-7s %s (esperado %s, obtenido %s)\n' "$1" "$2" "$3" "$4"
	fi
}

# bloqueado <caso> <descripción> <obtenido>: la función aún no existe en el servidor (501)
bloqueado() {
	if [ "$3" = "501" ]; then
		bloqueados=$((bloqueados + 1)); printf "BLOQUEADO %-7s %s (el servidor responde 501 Not Implemented)
" "$1" "$2"
	else
		verificar "$1" "$2" 501 "$3"
	fi
}

token() { awk '$6 == "XSRF-TOKEN" { print $7 }' "$1"; }

# sesion <tarro> <usuario> <clave>: obtiene CSRF e inicia sesión; devuelve el código HTTP del login
sesion() {
	curl -s -o /dev/null -c "$1" "$API/auth/csrf"
	local jq_body=$(printf '{"username":"%s","password":"%s"}' "$2" "$3")
	curl -s -o "$TMP/login.json" -w '%{http_code}' -b "$1" -c "$1" -H "X-XSRF-TOKEN: $(token "$1")" \
		-H 'Content-Type: application/json' -d "$jq_body" "$API/auth/login"
}

# estado <tarro> <método> <ruta> [cuerpo]: código HTTP de la solicitud (con CSRF si modifica)
estado() {
	local extra=() cuerpo="${4:-}"
	[ -n "$cuerpo" ] || cuerpo='{}'
	if [ "$2" != "GET" ]; then extra=(-H "X-XSRF-TOKEN: $(token "$1")" -H 'Content-Type: application/json' -d "$cuerpo"); fi
	curl -s -o "$TMP/respuesta.json" -w '%{http_code}' -b "$1" -c "$1" -X "$2" "${extra[@]}" "$API$3"
}

codigo() { grep -o '"code":"[^"]*"' "$TMP/respuesta.json" | cut -d'"' -f4; }

echo "Pruebas de API contra $API — $(date '+%Y-%m-%d %H:%M:%S')"
echo

verificar CP-016 "Login del planeador" 200 "$(sesion "$TMP/plan" planeador "$CLAVE_DEMO")"
verificar CP-016 "La sesión informa el rol PLANNER" PLANNER "$(grep -o '"role":"[A-Z]*"' "$TMP/login.json" | cut -d'"' -f4)"

verificar CP-017 "Contraseña incorrecta" 401 "$(sesion "$TMP/x1" planeador 'clave-incorrecta-123')"
cp "$TMP/login.json" "$TMP/clave.json"
verificar CP-017 "Usuario inexistente" 401 "$(sesion "$TMP/x2" no-existe 'clave-incorrecta-123')"
verificar CP-017 "Mismo mensaje para usuario inexistente y clave errónea" iguales \
	"$(cmp -s <(grep -o '"detail":"[^"]*"' "$TMP/clave.json") <(grep -o '"detail":"[^"]*"' "$TMP/login.json") && echo iguales || echo distintos)"

verificar CP-023 "Planeador consulta pronósticos (permitido; lógica pendiente)" 501 "$(estado "$TMP/plan" GET /forecasts)"
verificar CP-023 "Planeador intenta administrar cuentas" 403 "$(estado "$TMP/plan" GET /admin/users)"

sesion "$TMP/sup" supervisor "$CLAVE_DEMO" > /dev/null
verificar CP-025 "Supervisor intenta ejecutar un pronóstico" 403 "$(estado "$TMP/sup" POST /forecasts)"
verificar CP-025 "La denegación usa el código access-denied" access-denied "$(codigo)"
verificar CP-023 "Supervisor intenta cargar observaciones" 403 "$(estado "$TMP/sup" POST /history/series/1/observations)"

sesion "$TMP/ana" analista "$CLAVE_DEMO" > /dev/null
verificar CP-023 "Analista carga observaciones (permitido; lote vacío rechazado)" 400 "$(estado "$TMP/ana" POST /history/series/1/observations)"
verificar CP-023 "El lote vacío se informa por campo" validation "$(codigo)"

if [ -n "$ADMIN" ] && [ -n "$CLAVE_ADMIN" ]; then
	sesion "$TMP/adm" "$ADMIN" "$CLAVE_ADMIN" > /dev/null
	verificar CP-024 "Administrador lista cuentas" 200 "$(estado "$TMP/adm" GET /admin/users)"
	verificar CP-024 "Administrador no hereda permisos de históricos" 403 "$(estado "$TMP/adm" GET /history/products)"
fi

verificar CP-027 "Anónimo consulta pronósticos" 401 "$(estado "$TMP/anon" GET /forecasts)"
verificar CP-027 "Anónimo consulta su sesión" 401 "$(estado "$TMP/anon" GET /auth/me)"
verificar CP-027 "Ruta sin versión denegada" 401 "$(curl -s -o /dev/null -w '%{http_code}' "$BASE/api/auth/me")"

curl -s -o /dev/null -c "$TMP/sin" "$API/auth/csrf"
verificar CP-028 "Login sin cabecera CSRF" 403 "$(curl -s -o "$TMP/respuesta.json" -w '%{http_code}' -b "$TMP/sin" \
	-H 'Content-Type: application/json' -d '{"username":"planeador","password":"x"}' "$API/auth/login")"
verificar CP-028 "Rechazo con código csrf-invalid" csrf-invalid "$(codigo)"

verificar CP-030 "JSON malformado en el login" 400 "$(estado "$TMP/sin" POST /auth/login '{"username":')"
verificar CP-030 "Código malformed-request" malformed-request "$(codigo)"

verificar CP-031 "Consultar productos" 200 "$(estado "$TMP/plan" GET /history/products)"
verificar CP-031 "El catálogo incluye Jersey Pro" si "$(grep -q "\"code\":\"JERSEY-PRO\"" "$TMP/respuesta.json" && echo si || echo no)"

# Requiere los históricos cargados (docs/datos/README.md); cada ejecución queda guardada.
verificar "CP-001…009" "Ejecutar pronóstico MA(2) sobre Jersey Pro" 201 "$(estado "$TMP/plan" POST /forecasts '{"seriesId":1,"methods":[{"method":"SIMPLE_MOVING_AVERAGE","periods":2}]}')"
verificar "CP-001…009" "La ejecución trae MAD y MAPE" si "$(grep -q '"mad":[0-9]' "$TMP/respuesta.json" && grep -q '"mapePercent":[0-9]' "$TMP/respuesta.json" && echo si || echo no)"
verificar CP-025 "Supervisor consulta ejecuciones (solo lectura)" 200 "$(estado "$TMP/sup" GET /forecasts)"

echo
echo "Casos cuya función aún no está implementada (se reintentan cuando exista):"
bloqueado CP-032 "Consultar indicadores de una ejecución" "$(estado "$TMP/plan" GET /indicators/executions/1)"
echo

verificar CP-019 "Cierre de sesión" 204 "$(estado "$TMP/plan" POST /auth/logout)"
verificar CP-019 "Tras cerrar sesión /me ya no responde" 401 "$(estado "$TMP/plan" GET /auth/me)"

echo
echo "Resultado: $aprobados verificaciones aprobadas, $fallidos fallidas, $bloqueados bloqueadas"
[ "$fallidos" -eq 0 ]
