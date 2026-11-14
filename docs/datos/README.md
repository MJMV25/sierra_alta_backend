# Datos del sistema

## Datos de la empresa y datos de ejemplo (desde V10, 2026-11-14)

V10 vuelve a crear las 4 referencias de la empresa (que V7 había borrado para verificar contra el Excel) y
deja Spokane y ACME como **datos de ejemplo** (`producto.ejemplo`), de solo lectura, para la presentación
([ADR 13](../architecture/adr/0013-hoja-de-datos-en-vivo.md)). En la pantalla de inicio están en la pestaña
"Datos de ejemplo": se pueden cambiar valores para probar, pero nada se guarda.

| Código           | Caso                                   | Datos                    | Unidad   |
|------------------|----------------------------------------|--------------------------|----------|
| `CASO-1-SPOKANE` | Compras de gasolina de Spokane         | t = 1 a 30               | galones  |
| `CASO-2-ACME`    | ACME Tool Company                      | t = 1 a 25 (2000 T1–2006 T1) | unidades |

- Periodicidad `PERIODO`: la interfaz muestra "t = n" como el Excel; internamente t = 1 es 2000-01.
- Parámetros del Excel (hoja RESUMEN), que la pantalla carga solos al elegir cada caso:
  - Spokane: n = 5; pesos K0..K4 = 0,3 · 0,2 · 0,2 · 0,2 · 0,1; α = 0,05; Holt α = 0,0025, β = 0,99.
    MAD 20,584 · 19,800 · 16,252 · 16,061; MAPE 7,50 % · 7,22 % · 5,88 % · 5,79 %.
  - ACME: n = 4; pesos K0..K3 = 0,1 · 0,2 · 0,3 · 0,4; α = 0,6; Holt α = 0,3, β = 0,1.
    MAD 116,667 · 106,905 · 147,951 · 132,380; MAPE 29,59 % · 27,27 % · 37,19 % · 32,10 %.
  La prueba `ForecastApiTest.excelTestCasesLoadedByTheMigrationGiveTheSummarySheet` lo comprueba.

## Datos de la empresa

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

Hay dos formas; ambas pasan por las mismas reglas y quedan en el historial de cambios:

- **Desde la aplicación (recomendada):** como administrador, en la pantalla de inicio, "Importar Excel" con
  el Excel del proyecto (hoja DATOS: un bloque por año con REFERENCIA y ENERO…DICIEMBRE). Los meses entran a
  la hoja resaltados como cambios sin guardar; se revisan y se guardan con "Guardar cambios".
- **Con el script**, a partir de `datos-locales/produccion-mensual.csv` (carpeta en `.gitignore`), con una
  fila por referencia y mes:

  ```csv
  codigo,periodo,valor
  JERSEY-PRO,2022-01,0
  JERSEY-PRO,2022-02,21
  ```

  ```bash
  ADMIN_USUARIO=admin bash docs/datos/cargar-historicos.sh
  ```

  El script pide la contraseña sin mostrarla y guarda las 4 referencias juntas con
  `PATCH /api/v1/history/observations`. Al volver a ejecutarlo no cambia nada (los valores ya son iguales).

Un guion del Excel es 0 unidades (respuesta 14 de Industrial). Son 57 meses por referencia.

## Meses nuevos y correcciones

Solo el administrador edita la hoja (ADR 13): escribe el mes nuevo en la celda o corrige uno existente y
presiona "Guardar cambios". Reglas: unidades enteras ≥ 0, sin meses futuros, sin huecos (un mes sin
producción se registra con 0) y solo se pueden borrar los últimos meses. Cada guardado crea una nueva versión
de datos; los pronósticos guardados antes no cambian.
