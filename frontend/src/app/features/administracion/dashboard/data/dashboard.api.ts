import { environment } from '@env/environment';

const base = `${environment.apiGatewayUrl}/admin/metricas`;

export const apiAdminMetricas =
{
    get:
    {
        diarias: `${base}/diarias`,
        ranking: `${base}/ranking`,
        ultimos: `${base}/ultimos`,
    },
};
