import { environment } from '@env/environment';

const base = `${environment.apiGatewayUrl}/admin/rabbit`;

export const apiAdminRabbit =
{
    get:
    {
        queues: `${base}/queues`,
        exchanges: `${base}/exchanges`,
        bindings: `${base}/bindings`,
        overview: `${base}/overview`,
    },
    post:
    {
        queues: `${base}/queues`,
        exchanges: `${base}/exchanges`,
        bindings: `${base}/bindings`,
        demoFalloControlado: `${base}/demo/fallo-controlado`,
        demoTtl: `${base}/demo/ttl`,
    },
    delete:
    {
        queue: (name: string) => `${base}/queues/${encodeURIComponent(name)}`,
        exchange: (name: string) => `${base}/exchanges/${encodeURIComponent(name)}`,
        bindings: `${base}/bindings`,
    },
};
