export const environment = {
  production: false,

  msal: {
    // App Registration del FRONTEND Angular
    clientId: '9da5f704-8e4f-493b-af75-812436921593',

    // Directory (tenant) ID del tenant utilizado en Microsoft Entra ID
    tenantId: '1f3a849e-c198-4ff7-b67c-d17f15cbc152',

    redirectUri: 'http://localhost:4200',

    // Scope expuesto por la App Registration de la API
    apiScope:
      'api://374ba786-74ed-4b20-a6f5-b115c2e58625/Pedidos.Read'
  },

  // Backend Spring Boot de la Sesión 3
  apiBaseUrl: 'http://localhost:8080'
};
