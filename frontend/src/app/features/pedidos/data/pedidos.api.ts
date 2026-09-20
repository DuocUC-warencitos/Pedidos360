import { environment } from '@env/environment';

const base = `${environment.apiGatewayUrl}/api/v1/pedidos`;
const sagaBase = `${base}/saga`;

export const apiPedidos = {
	get: {
		all: base/all,
		jobStatus: (jobId: string) => `${sagaBase}/jobs/${jobId}`,
	},
	post: {
		/** @deprecated usar createSaga (201 síncrono sin reserva de stock). */
		create: base,
		createSaga: sagaBase,
	},
	delete: {
		all: `${base}/all`,
	},
	patch: {
		changeStatus: (id: string) => `${base}/${id}/avanzarEstado`,
		cancel: (id: string) => `${base}/${id}/cancelar`,
	},
} as const;
