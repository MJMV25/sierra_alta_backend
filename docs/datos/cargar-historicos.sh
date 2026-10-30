#!/usr/bin/env bash
# Carga los históricos de producción por la API, una carga por referencia (ver docs/datos/README.md).
# Uso:  bash docs/datos/cargar-historicos.sh [CSV] [URL_BASE]
#       CSV       por defecto datos-locales/produccion-mensual.csv (columnas codigo,periodo,valor)
#       URL_BASE  por defecto http://localhost:$SERVER_PORT
# Credenciales: ANALISTA_USUARIO (por defecto "analista") y ANALISTA_CLAVE; si falta la clave, la pide
# sin mostrarla. Se usa la API y no SQL para que cada carga pase las mismas validaciones y quede auditada.
set -u

RAIZ="$(cd "$(dirname "$0")/../.." && pwd)"
leer_env() { [ -f "$RAIZ/.env" ] && grep -E "^$1=" "$RAIZ/.env" | head -1 | cut -d= -f2- | tr -d '\r'; }

CSV="${1:-$RAIZ/datos-locales/produccion-mensual.csv}"
PUERTO="$(leer_env SERVER_PORT)"
API="${2:-http://localhost:${PUERTO:-8080}}/api/v1"
USUARIO="${ANALISTA_USUARIO:-analista}"
if [ -z "${ANALISTA_CLAVE:-}" ]; then
	read -r -s -p "Contraseña de $USUARIO: " ANALISTA_CLAVE
	echo
fi

[ -f "$CSV" ] || { echo "No existe el archivo $CSV" >&2; exit 1; }
TMP="$(mktemp -d)"
trap 'rm -rf "$TMP"' EXIT
token() { awk '$6 == "XSRF-TOKEN" { print $7 }' "$TMP/tarro"; }

curl -s -o /dev/null -c "$TMP/tarro" "$API/auth/csrf"
login=$(printf '{"username":"%s","password":"%s"}' "$USUARIO" "$ANALISTA_CLAVE")
estado=$(curl -s -o /dev/null -w '%{http_code}' -b "$TMP/tarro" -c "$TMP/tarro" -H "X-XSRF-TOKEN: $(token)" \
	-H 'Content-Type: application/json' -d "$login" "$API/auth/login")
[ "$estado" = "200" ] || { echo "No se pudo iniciar sesión como $USUARIO (HTTP $estado)" >&2; exit 1; }

curl -s -b "$TMP/tarro" -o "$TMP/productos.json" "$API/history/products"
fallidas=0
for codigo in $(tail -n +2 "$CSV" | cut -d, -f1 | tr -d '\r' | sort -u); do
	serie=$(grep -o "\"code\":\"$codigo\",\"name\":\"[^\"]*\",\"series\":\[{\"id\":[0-9]*" "$TMP/productos.json" \
		| grep -o '[0-9]*$')
	if [ -z "$serie" ]; then
		echo "OMITIDA  $codigo: no existe en el catálogo"; fallidas=$((fallidas + 1)); continue
	fi
	tail -n +2 "$CSV" | tr -d '\r' | awk -F, -v c="$codigo" '
		$1 == c { printf "%s{\"period\":\"%s\",\"value\":%s}", (n++ ? "," : "{\"observations\":["), $2, $3 }
		END { print "]}" }' > "$TMP/cuerpo.json"
	estado=$(curl -s -o "$TMP/respuesta.json" -w '%{http_code}' -b "$TMP/tarro" -c "$TMP/tarro" \
		-H "X-XSRF-TOKEN: $(token)" -H 'Content-Type: application/json' -d @"$TMP/cuerpo.json" \
		"$API/history/series/$serie/observations")
	if [ "$estado" = "201" ]; then
		echo "CARGADA  $codigo (serie $serie): $(grep -o '"observations":[0-9]*' "$TMP/respuesta.json" | cut -d: -f2) meses"
	else
		fallidas=$((fallidas + 1))
		echo "RECHAZADA $codigo (serie $serie), HTTP $estado: $(grep -o '"detail":"[^"]*"' "$TMP/respuesta.json" | cut -d'"' -f4)"
	fi
done

curl -s -o /dev/null -b "$TMP/tarro" -X POST -H "X-XSRF-TOKEN: $(token)" "$API/auth/logout"
[ "$fallidas" -eq 0 ]
