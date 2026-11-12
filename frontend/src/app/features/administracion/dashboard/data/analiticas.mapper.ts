import { AuditoriaItem, AuditoriaItemResponse } from './dashboard.types';

/**
 * Normaliza el contrato HTTP de auditoría al modelo de dominio.
 *
 * El backend omite las propiedades nulas al serializar, de modo que una
 * referencia ausente llega como `undefined`. En el dominio usamos siempre
 * `null` para representar "sin valor" y así evitar que las plantillas y
 * helpers reciban datos con dos representaciones distintas.
 */
export function normalizarAuditoria(item: AuditoriaItemResponse): AuditoriaItem
{
	return {
		eventoId: item.eventoId,
		tipo: item.tipo,
		productoId: item.productoId,
		nombre: item.nombre ?? null,
		sku: item.sku ?? null,
		pedidoId: item.pedidoId ?? null,
		operacionId: item.operacionId ?? null,
		cantidad: item.cantidad,
		stockDisponibleAntes: item.stockDisponibleAntes ?? null,
		stockDisponibleDespues: item.stockDisponibleDespues ?? null,
		stockReservadoAntes: item.stockReservadoAntes ?? null,
		stockReservadoDespues: item.stockReservadoDespues ?? null,
		ocurridoEn: item.ocurridoEn,
		correlationId: item.correlationId ?? null,
		usuarioId: item.usuarioId ?? null,
	};
}
