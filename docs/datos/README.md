# Históricos de producción

Las cantidades producidas de Sierra Alta **no se versionan** en este repositorio (igual que los Excel de
la empresa). La migración `V3__cargas_y_catalogo.sql` solo crea el catálogo: las 4 referencias de la
hoja DATOS, cada una con su serie de producción mensual en unidades.

| Código                | Referencia          |
|-----------------------|---------------------|
| `JERSEY-PRO`          | Jersey Pro          |
| `PANTALONETA-ESTELAR` | Pantaloneta Estelar |
| `T-SHIRT-RUNNING`     | T-Shirt Running     |
| `JERSEY-SLIM`         | Jersey Slim         |

## Carga inicial (enero de 2022 a septiembre de 2026)

1. Prepare `datos-locales/produccion-mensual.csv` (la carpeta está en `.gitignore`) a partir de la hoja
   DATOS del Excel corregido, con una fila por referencia y mes:

   ```csv
   codigo,periodo,valor
   JERSEY-PRO,2022-01,0
   JERSEY-PRO,2022-02,21
   ```

   Un guion del Excel es 0 unidades (respuesta 14 de Industrial). Son 57 meses por referencia.
2. Con el backend en marcha, ejecute:

   ```bash
   ANALISTA_USUARIO=analista bash docs/datos/cargar-historicos.sh
   ```

   El script pide la contraseña sin mostrarla y hace una carga por referencia con
   `POST /api/v1/history/series/{id}/observations`. Al volver a ejecutarlo, las referencias ya cargadas
   salen rechazadas con HTTP 409 y no se duplica nada.

## Meses nuevos

El analista agrega cada mes cerrado con el mismo endpoint (o el mismo script con un CSV de un solo mes).
Reglas (ADR 07): meses consecutivos, sin repetir uno ya cargado, sin meses futuros, cantidades ≥ 0 con
hasta 4 decimales y hasta 240 meses por carga. La carga entra completa o se rechaza completa.
