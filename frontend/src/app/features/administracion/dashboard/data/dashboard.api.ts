import { environment } from '@env/environment';

const base = `${environment.apiGatewayUrl}/admin/metricas`;
const baseAnaliticas = `${environment.apiGatewayUrl}/v1/admin/analiticas`;

export const apiAdminMetricas =
{
    get:
    {
        diarias: `${base}/diarias`,
        ranking: `${base}/ranking`,
        ultimos: `${base}/ultimos`,
    },
};

export const apiAdminAnaliticas =
{
    get:
    {
        resumen: `${baseAnaliticas}/inventario/resumen`,
        alertas: `${baseAnaliticas}/inventario/alertas`,
        top: `${baseAnaliticas}/inventario/top`,
        movimientos: `${baseAnaliticas}/inventario/movimientos`,
        auditoria: `${baseAnaliticas}/auditoria`,
        estado: `${baseAnaliticas}/estado`,
    },
};