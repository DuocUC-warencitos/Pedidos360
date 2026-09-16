import { environment } from '@env/environment';

const base = `${environment.apiGatewayUrl}/api/v1/pedidos`;

export const apiPedidos = {
  get: {
    all: base,
  },
  post: {
    create: base,
  },
  delete: {
    all: `${base}/all`,
  },
  patch: {
    changeStatus: (id: string) => `${base}/${id}/avanzarEstado`,
    cancel: (id: string) => `${base}/${id}/cancelar`,
  },
};
