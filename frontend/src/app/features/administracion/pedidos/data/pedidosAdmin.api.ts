import { environment } from '@env/environment';

const base = `${environment.apiGatewayUrl}/v1/admin/pedidos`;

export const apiAdminPedidos = {
  get: {
    findAll: base,
    stats: `${base}/stats`,
    byUserId: (userId: string) => `${base}/usuario/${encodeURIComponent(userId)}`,
    byId: (id: string) => `${base}/${id}`,
  },
  patch: {
    cancelById: (id: string) => `${base}/${id}/cancelar`,
    avanzarById: (id: string) => `${base}/${id}/avanzarEstado`,
  },
} as const;
