export interface PuntoDiario {
	fecha: string;
	routingKey: string;
	total: number;
}

export interface ProductoRanking {
	productoId: string;
	total: number;
}

export interface ProductoRankingConNombre extends ProductoRanking {
	nombre: string | null;
}

export interface EventoReciente {
	recibidoAt: string;
	routingKey: string;
	pedidoId: string | null;
	correlationId: string | null;
}

export interface KpiMes {
	etiqueta: string;
	valor: string;
	delta: number | null;
	detalle: string;
}

export interface MesAgrupado {
	clave: string;
	etiqueta: string;
	total: number;
}

export interface DiaAgrupado {
	fecha: string;
	etiqueta: string;
	total: number;
}

export interface DesgloseTipo {
	routingKey: string;
	total: number;
	porcentaje: number;
}

export interface PeriodoRanking {
	desde: string;
	hasta: string;
}

export interface EstadoStreamsResponse {
	estado: string;
	stores: string[];
	productos: number;
	movimientos: number;
	eventos: number;
}

export interface ResumenAnaliticaResponse {
    total: number;
    activos: number;
    stockTotal: number;
    stockReservadoTotal: number;
    alertas: number;
    unidadesConfirmadasMes: number;
    ingresosConfirmadosMes: number;
}

/**
 * Contrato HTTP de auditoría.
 *
 * El backend serializa con `spring.jackson.default-property-inclusion: non_null`,
 * por lo que las propiedades sin valor viajan ausentes (no como `null`). Por eso
 * las referencias opcionales se declaran opcionales aquí y se normalizan en
 * `normalizarAuditoria()` antes de entrar al dominio de la aplicación.
 */
export interface AuditoriaItemResponse {
	eventoId: string;
	tipo: string;
	productoId: string;
	nombre?: string | null;
	sku?: string | null;
	pedidoId?: string | null;
	operacionId?: string | null;
	cantidad: number;
	stockDisponibleAntes?: number | null;
	stockDisponibleDespues?: number | null;
	stockReservadoAntes?: number | null;
	stockReservadoDespues?: number | null;
	ocurridoEn: string;
	correlationId?: string | null;
	usuarioId?: string | null;
}

export interface AuditoriaItem {
	eventoId: string;
	tipo: string;
	productoId: string;
	nombre: string | null;
	sku: string | null;
	pedidoId: string | null;
	operacionId: string | null;
	cantidad: number;
	stockDisponibleAntes: number | null;
	stockDisponibleDespues: number | null;
	stockReservadoAntes: number | null;
	stockReservadoDespues: number | null;
	ocurridoEn: string;
	correlationId: string | null;
	usuarioId: string | null;
}

export interface MovimientoDia {
	fecha: string;
	tipo: string;
	unidades: number;
	ingresos: number;
	operaciones: number;
}

export interface AlertaStock {
	id: string;
	nombre: string;
	sku: string;
	stockDisponible: number;
	stockMinimo: number | null;
}

export interface TopProducto {
    productoId: string;
    nombre: string;
    sku: string;
    unidades: number;
    ingresos: number;
}