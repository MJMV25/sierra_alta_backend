"""Recorrido real React/API/PostgreSQL, sobre una base QA previamente migrada."""
import json
import os
import re
from pathlib import Path
from playwright.sync_api import sync_playwright, expect

out = Path('target/e2e')
out.mkdir(parents=True, exist_ok=True)
base = os.environ.get('QA_URL', 'http://localhost:15173')
results = []

def check(name, condition):
    results.append({'caso': name, 'aprobado': bool(condition)})
    assert condition, name

with sync_playwright() as p:
    launch = {'headless': True}
    if os.environ.get('QA_BROWSER_CHANNEL'):
        launch['channel'] = os.environ['QA_BROWSER_CHANNEL']
    browser = p.chromium.launch(**launch)
    context = browser.new_context(accept_downloads=True)
    page = context.new_page()
    page.set_default_timeout(30000)
    try:
        page.goto(base + '/login')
        page.locator('#username').fill('admin')
        page.locator('#password').fill(os.environ.get('QA_ADMIN_PASSWORD', 'admin-password-123'))
        page.locator('button[type=submit]').click()
        expect(page.locator('nav').first).to_be_visible()
        page.goto(base + '/historicos')
        page.locator('#upload-series').select_option('1')
        page.locator('#upload-text').fill('\n'.join(f'2020-{m:02d};{100 + m * 10}' for m in range(1, 13)))
        page.get_by_role('button', name='Cargar históricos', exact=True).click()
        with page.expect_response(lambda r: '/observations' in r.url and r.request.method == 'POST') as response:
            page.get_by_role('button', name='Cargar', exact=True).click()
        check('Carga desde React', response.value.status == 201)
        page.goto(base + '/pronosticos')
        page.locator('#forecast-series').select_option('1')
        with page.expect_response(lambda r: '/api/v1/forecasts' in r.url and r.request.method == 'POST') as response:
            page.get_by_role('button', name='Ejecutar pronóstico', exact=True).click()
        check('Pronóstico desde React', response.value.status == 201)
        data = response.value.json()
        check('Cuatro métodos', len(data['methods']) == 4)
        # MA(3) sobre una progresión de paso 10: cada error es 20, MSE=400.
        simple = next(m for m in data['methods'] if m['method'] == 'SIMPLE_MOVING_AVERAGE')
        check('MSE independiente MA(3)', abs(simple['mse'] - 400) < 1e-6)
        page.wait_for_url(re.compile(r'/pronosticos/\d+$'))
        expect(page.get_by_text('Comparación de métodos por MAD y MAPE', exact=True)).to_be_attached()
        page.screenshot(path=str(out / 'react-api-postgresql.png'), full_page=True)
        reread = context.request.get(base + '/api/v1/forecasts/' + str(data['id']))
        check('Relectura persistida', reread.status == 200 and reread.json()['methods'] == data['methods'])
        with page.expect_download(timeout=60000) as download:
            page.get_by_role('button', name='Exportar a Excel', exact=True).click()
        download.value.save_as(str(out / 'exportacion-real.xlsx'))
        check('Exportación real Excel', (out / 'exportacion-real.xlsx').stat().st_size > 0)
        (out / 'resultado-api.json').write_text(json.dumps(data, indent=2), encoding='utf-8')
    except Exception:
        page.screenshot(path=str(out / 'fallo.png'), full_page=True)
        raise
    finally:
        (out / 'resultados.json').write_text(json.dumps(results, indent=2), encoding='utf-8')
        browser.close()
