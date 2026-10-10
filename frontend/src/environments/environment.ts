import { LogLevel } from "../app/core/logging/logLevel";

declare global 
{
    interface Window 
    {
        __env: 
        {
            MSAL_CLIENT_ID: string;
            MSAL_TENANT_ID: string;
            MSAL_REDIRECT_URI: string;
            MSAL_API_SCOPE: string;
            MSAL_POST_LOGOUT_URI: string;
            API_GATEWAY_URL: string;
            ANGULAR_LOG_LEVEL: string;
            ANGULAR_IS_PRODUCTION: string;
        };
    }
}

export const environment =
{
    production: window.__env?.ANGULAR_IS_PRODUCTION === "true",

    msal:
    {
        clientId: window.__env?.MSAL_CLIENT_ID,
        tenantId: window.__env?.MSAL_TENANT_ID,
        redirectUri: window.__env?.MSAL_REDIRECT_URI ?? window.location.origin,
        apiScope: window.__env?.MSAL_API_SCOPE,
        postLogoutRedirectUri: window.__env?.MSAL_POST_LOGOUT_URI ?? window.location.origin
    },

    apiGatewayUrl: window.__env?.API_GATEWAY_URL,

    logging:
    {
        minLevel:
            LogLevel[
                (window.__env?.ANGULAR_LOG_LEVEL) as keyof typeof LogLevel
            ] ?? LogLevel.INFO
    }
};
