#!/usr/bin/env bash
# Carga los históricos de producción por la API, como un guardado de la hoja de datos (ver
# docs/datos/README.md). La aplicación también lo permite con "Importar Excel" en la pantalla de inicio.
# Uso:  bash docs/datos/cargar-historicos.sh [CSV] [URL_BASE]
#       CSV       por defecto datos-locales/produccion-mensual.csv (columnas codigo,periodo,valor)
#       URL_BASE  por defecto http://localhost:$SERVER_PORT
# Credenciales: ADMIN_USUARIO (por defecto "admin") y ADMIN_CLAVE; si falta la clave, la pide sin
# mostrarla. Solo el administrador edita la hoja (ADR 13). Se usa la API y no SQL para que cada mes pase
# las mismas validaciones y quede en el historial de cambios.
set -u

RAIZ="$(cd "$(dirname "$0")/../.." && pwd)"
leer_env() { [ -f "$RAIZ/.env" ] && grep -E "^$1=" "$RAIZ/.env" | head -1 | cut -d= -f2- | tr -d '\r'; }

CSV="${1:-$RAIZ/datos-locales/produccion-mensual.csv}"
PUERTO="$(leer_env SERVER_PORT)"
API="${2:-http://localhost:${PUERTO:-8080}}/api/v1"
USUARIO="${ADMIN_USUARIO:-admin}"
if [ -z "${ADMIN_CLAVE:-}" ]; then
	read -r -s -p "Contraseña de $USUARIO: " ADMIN_CLAVE
	echo
fi

[ -f "$CSV" ] || { echo "No existe el archivo $CSV" >&2; exit 1; }
TMP="$(mktemp -d)"
trap 'rm -rf "$TMP"' EXIT
token() { awk '$6 == "XSRF-TOKEN" { print $7 }' "$TMP/tarro"; }

curl -s -o /dev/null -c "$TMP/tarro" "$API/auth/csrf"
login=$(printf '{"username":"%s","password":"%s"}' "$USUARIO" "$ADMIN_CLAVE")
estado=$(curl -s -o /dev/null -w '%{http_code}' -b "$TMP/tarro" -c "$TMP/tarro" -H "X-XSRF-TOKEN: $(token)" \
	-H 'Content-Type: application/json' -d "$login" "$API/auth/login")
[ "$estado" = "200" ] || { echo "No se pudo iniciar sesión como $USUARIO (HTTP $estado)" >&2; exit 1; }

curl -s -b "$TMP/tarro" -o "$TMP/productos.json" "$API/history/products"
series=""
for codigo in $(tail -n +2 "$CSV" | cut -d, -f1 | tr -d '\r' | sort -u); do
	serie=$(grep -o "\"code\":\"$codigo\",\"name\":\"[^\"]*\",\"sample\":false,\"series\":\[{\"id\":[0-9]*" \
		"$TMP/productos.json" | grep -o '[0-9]*$')
	if [ -z "$serie" ]; then
		echo "OMITIDA  $codigo: no existe en el catálogo de la empresa"; continue
	fi
	cambios=$(tail -n +2 "$CSV" | tr -d '\r' | awk -F, -v c="$codigo" '
		$1 == c { printf "%s{\"period\":\"%s\",\"value\":%s}", (n++ ? "," : ""), $2, $3 }')
	series="$series${series:+,}{\"seriesId\":$serie,\"changes\":[$cambios]}"
done
[ -n "$series" ] || { echo "No hay referencias para cargar" >&2; exit 1; }
printf '{"series":[%s]}' "$series" > "$TMP/cuerpo.json"

# Todas las referencias se guardan juntas: si una tiene un error, no se guarda ninguna.
estado=$(curl -s -o "$TMP/respuesta.json" -w '%{http_code}' -b "$TMP/tarro" -c "$TMP/tarro" -X PATCH \
	-H "X-XSRF-TOKEN: $(token)" -H 'Content-Type: application/json' -d @"$TMP/cuerpo.json" \
	"$API/history/observations")
if [ "$estado" = "200" ]; then
	echo "GUARDADA la hoja: $(cat "$TMP/respuesta.json")"
else
	echo "RECHAZADA, HTTP $estado: $(grep -o '"detail":"[^"]*"' "$TMP/respuesta.json" | cut -d'"' -f4)"
fi

curl -s -o /dev/null -b "$TMP/tarro" -X POST -H "X-XSRF-TOKEN: $(token)" "$API/auth/logout"
[ "$estado" = "200" ]
