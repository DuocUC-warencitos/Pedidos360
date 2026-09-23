#!/usr/bin/env node
/**
 * Genera src/environments/environment.ts desde variables de entorno.
 * - Si existe .env (local), lo carga con dotenv (opcional).
 * - Si no hay env vars, mantiene el hardcode de dev (PCs públicos sin .env).
 * Uso local: cp .env.example .env && node scripts/generate-environment.js
 * En Vercel: setear vars NG_APP_* en dashboard, el script corre en prebuild.
 */
const fs = require('fs');
const path = require('path');

const envPath = path.join(__dirname, '..', '.env');
const envExamplePath = path.join(__dirname, '..', '.env.example');
const targetPath = path.join(__dirname, '..', 'src', 'environments', 'environment.ts');

// Cargar .env si existe (sin dependencia obligatoria)
function loadDotEnv(file) {
  if (!fs.existsSync(file)) return;
  const content = fs.readFileSync(file, 'utf8');
  for (const line of content.split('\n')) {
    const trimmed = line.trim();
    if (!trimmed || trimmed.startsWith('#')) continue;
    const eq = trimmed.indexOf('=');
    if (eq === -1) continue;
    const key = trimmed.slice(0, eq).trim();
    let val = trimmed.slice(eq + 1).trim();
    // quitar comillas
    if ((val.startsWith('"') && val.endsWith('"')) || (val.startsWith("'") && val.endsWith("'"))) {
      val = val.slice(1, -1);
    }
    if (!(key in process.env)) process.env[key] = val;
  }
}

loadDotEnv(envPath);

// Detectar si hay al menos una NG_APP_* seteada
const hasEnv = ['NG_APP_MSAL_CLIENT_ID', 'NG_APP_MSAL_TENANT_ID', 'NG_APP_API_GATEWAY_URL'].some(k => process.env[k]);

if (!hasEnv) {
  console.log('[generate-environment] No NG_APP_* env vars found — manteniendo environment.ts hardcodeado (dev rápido)');
  process.exit(0);
}

const get = (k, fallback) => process.env[k] || fallback;

const clientId = get('NG_APP_MSAL_CLIENT_ID', '9da5f704-8e4f-493b-af75-812436921593');
const tenantId = get('NG_APP_MSAL_TENANT_ID', '1f3a849e-c198-4ff7-b67c-d17f15cbc152');
const redirectUri = get('NG_APP_MSAL_REDIRECT_URI', 'http://localhost:4200');
const apiScope = get('NG_APP_MSAL_API_SCOPE', 'api://374ba786-74ed-4b20-a6f5-b115c2e58625/Pedidos.Read');
const apiGatewayUrl = get('NG_APP_API_GATEWAY_URL', 'http://localhost:8080');
const logLevel = get('NG_APP_LOG_LEVEL', 'DEBUG'); // DEBUG | INFO | WARN | ERROR
const production = get('NG_APP_PRODUCTION', 'false') === 'true' ? 'true' : 'false';

const content = `import { LogLevel } from "../app/core/logging/logLevel";

export const environment = 
{
    production: ${production},
    msal: 
    {
        clientId: '${clientId}',
        tenantId: '${tenantId}',
        redirectUri: '${redirectUri}',
        apiScope: '${apiScope}'
    },
    apiGatewayUrl: '${apiGatewayUrl}',
    logging:
    {
        minLevel: LogLevel.${logLevel}
    }
};
`;

fs.mkdirSync(path.dirname(targetPath), { recursive: true });
fs.writeFileSync(targetPath, content, 'utf8');
console.log(`[generate-environment] environment.ts generado desde env vars -> ${targetPath}`);
