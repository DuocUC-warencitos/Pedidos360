// Requests
export interface PedidoProductoRequest
{
	nombreProducto: string;
	cantidad: number;
}

export type EstadoPedido =
    | 'CREADO'
    | 'CONFIRMADO'
    | 'EN_PREPARACION'
    | 'DESPACHADO'
    | 'ENTREGADO'
    | 'CANCELADO';

// Responses
export interface PedidoResponse
{
	id: number;
	userId: string;
	productos: PedidoProductoResponse[];
	estado: EstadoPedido;
	createdAt: string;
	updatedAt: string;
}

export interface PedidoProductoResponse
{
	nombreProducto: string;
	cantidad: number;
}