import { environment } from '@env/environment';

const base = `${environment.apiGatewayUrl}/notificaciones`;

export const apiNotificaciones =
{
    usuario: `${base}/usuario`,
    eliminar: (id: number): string => `${base}/${id}`,
};
