import { readFileSync, writeFileSync } from 'node:fs';
import { fileURLToPath } from 'node:url';

const KEYS = [
  'MSAL_CLIENT_ID',
  'MSAL_TENANT_ID',
  'MSAL_REDIRECT_URI',
  'MSAL_POST_LOGOUT_URI',
  'MSAL_API_SCOPE',
  'API_GATEWAY_URL',
  'ANGULAR_LOG_LEVEL',
  'ANGULAR_IS_PRODUCTION'
];

const envPath = fileURLToPath(new URL('../public/env.js', import.meta.url));

let current = {};
try {
  const code = readFileSync(envPath, 'utf8');
  const sandboxWindow = {};
  new Function('window', code)(sandboxWindow);
  current = sandboxWindow.__env ?? {};
} catch {
  current = {};
}

const merged = {};
let fromEnv = 0;
for (const key of KEYS) {
  if (process.env[key]) {
    merged[key] = process.env[key];
    fromEnv++;
  } else if (current[key] !== undefined) {
    merged[key] = current[key];
  } else {
    merged[key] = '';
  }
}

if (fromEnv === 0) {
  console.log('[set-env] Sin variables de entorno; se mantiene public/env.js actual.');
  if (process.env.VERCEL) {
    console.warn('[set-env] ADVERTENCIA: deploy en Vercel sin variables (API_GATEWAY_URL, MSAL_REDIRECT_URI, ...): el front quedara apuntando a localhost.');
  }
  process.exit(0);
}

if (!process.env.MSAL_POST_LOGOUT_URI && process.env.MSAL_REDIRECT_URI) {
  merged.MSAL_POST_LOGOUT_URI = process.env.MSAL_REDIRECT_URI;
}

writeFileSync(envPath, `window.__env = ${JSON.stringify(merged, null, 4)};\n`, 'utf8');
console.log(`[set-env] public/env.js actualizado (${fromEnv} variables de entorno aplicadas).`);

if (merged.API_GATEWAY_URL && merged.API_GATEWAY_URL.includes('localhost')) {
  console.warn('[set-env] ADVERTENCIA: API_GATEWAY_URL sigue apuntando a localhost.');
}
