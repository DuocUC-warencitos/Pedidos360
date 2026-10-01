#!/bin/sh

cat <<EOF > /usr/share/nginx/html/env.js
window.__env = {
  MSAL_CLIENT_ID: "${MSAL_CLIENT_ID}",
  MSAL_TENANT_ID: "${MSAL_TENANT_ID}",
  MSAL_REDIRECT_URI: "${MSAL_REDIRECT_URI}",
  MSAL_API_SCOPE: "${MSAL_API_SCOPE}",
  API_GATEWAY_URL: "${API_GATEWAY_URL}",
  ANGULAR_LOG_LEVEL: "${ANGULAR_LOG_LEVEL}",
  ANGULAR_IS_PRODUCTION: "${ANGULAR_IS_PRODUCTION}"
};
EOF

exec nginx -g "daemon off;"