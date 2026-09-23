# syntax=docker/dockerfile:1
# Build stage
FROM node:20-alpine AS build
WORKDIR /app
COPY package.json package-lock.json ./
RUN npm ci || npm install
COPY . .
# Build args para generate-environment.js (build-time)
ARG NG_APP_MSAL_CLIENT_ID
ARG NG_APP_MSAL_TENANT_ID
ARG NG_APP_MSAL_REDIRECT_URI
ARG NG_APP_MSAL_API_SCOPE
ARG NG_APP_API_GATEWAY_URL
ARG NG_APP_LOG_LEVEL
ARG NG_APP_PRODUCTION
ENV NG_APP_MSAL_CLIENT_ID=$NG_APP_MSAL_CLIENT_ID \
    NG_APP_MSAL_TENANT_ID=$NG_APP_MSAL_TENANT_ID \
    NG_APP_MSAL_REDIRECT_URI=$NG_APP_MSAL_REDIRECT_URI \
    NG_APP_MSAL_API_SCOPE=$NG_APP_MSAL_API_SCOPE \
    NG_APP_API_GATEWAY_URL=$NG_APP_API_GATEWAY_URL \
    NG_APP_LOG_LEVEL=$NG_APP_LOG_LEVEL \
    NG_APP_PRODUCTION=$NG_APP_PRODUCTION
RUN npm run build -- --configuration production

# Runtime stage
FROM nginx:alpine
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist/dsy1107-msal-angular/browser /usr/share/nginx/html
EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
