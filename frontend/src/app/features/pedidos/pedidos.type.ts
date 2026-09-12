// Requests
export interface PedidoProductoRequest
{
	nombreProducto: string;
	cantidad: number;
}
// Responses
export interface PedidoResponse
{
	id: number;
	userId: string;
	productos: PedidoProductoResponse[];
	createdAt: string;
	updatedAt: string;
}

export interface PedidoProductoResponse
{
	nombreProducto: string;
	cantidad: number;
}