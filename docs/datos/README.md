# Datos del sistema

## Fase de verificación: casos de prueba del Excel (desde V7, 2026-11-06)

Ingeniería Industrial pidió que la aplicación trabaje con los mismos datos del Excel para verificar que
los cálculos coinciden. La migración `V7__datos_de_prueba_del_excel.sql` **borra todo lo de Sierra Alta**
(pronósticos, cargas, observaciones y las 4 referencias) y deja solo los dos casos de la hoja DATOS de
"EXCEL CORREGIDO DEL PROYECTO":

| Código           | Caso                                   | Datos                    | Unidad   |
|------------------|----------------------------------------|--------------------------|----------|
| `CASO-1-SPOKANE` | Compras de gasolina de Spokane         | t = 1 a 30               | galones  |
| `CASO-2-ACME`    | ACME Tool Company                      | t = 1 a 25 (2000 T1–2006 T1) | unidades |

- Periodicidad `PERIODO`: la interfaz muestra "t = n" como el Excel; internamente t = 1 es 2000-01,
  t = 2 es 2000-02, etc. CSV y Excel exportados traen la columna t.
- Son de solo lectura: el backend rechaza cargas en ellas. La carga figura a nombre de la cuenta técnica
  `sistema` (inactiva, sin contraseña conocida).
- Parámetros del Excel para verificar (hoja RESUMEN):
  - Spokane: n = 5; pesos K0..K4 = 0,3 · 0,2 · 0,2 · 0,2 · 0,1; α = 0,05; Holt α = 0,0025, β = 0,99.
    MAD 20,584 · 19,800 · 16,252 · 16,061; MAPE 7,50 % · 7,22 % · 5,88 % · 5,79 %.
  - ACME: n = 4; pesos K0..K3 = 0,1 · 0,2 · 0,3 · 0,4; α = 0,6; Holt α = 0,3, β = 0,1.
    MAD 116,667 · 106,905 · 147,951 · 132,380; MAPE 29,59 % · 27,27 % · 37,19 % · 32,10 %.
  La prueba `ForecastApiTest.excelTestCasesLoadedByTheMigrationGiveTheSummarySheet` lo comprueba.

Cuando el sistema quede verificado, una nueva migración volverá a crear las referencias de la empresa y
sus datos se cargarán con el procedimiento de abajo.

## Datos de la empresa (antes de V7; para cuando se retomen)

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
