// Requests
export interface PedidoProductoRequest {
  nombreProducto: string;
  cantidad: number;
}

export interface CrearPedidoRequest
{
  items: ItemPedido[]
}

export interface ItemPedido
{
  productoId: string;
  cantidad: number;
}

export type EstadoJob = 'RUNNING' | 'COMPLETED' | 'FAILED';

export interface PedidoJobStatusResponse
{
  jobId: string;
  pedidoId: string;
  estadoJob: EstadoJob;
  estadoPedido: EstadoPedido;
  error: string | null;
  startedAt: string;
  finishedAt: string | null;
}

export type EstadoPedido =
  | 'CREADO'
  | 'STOCK_RESERVADO'
  | 'STOCK_FALLIDO'
  | 'CONFIRMADO'
  | 'EN_PREPARACION'
  | 'DESPACHADO'
  | 'ENTREGADO'
  | 'CANCELADO';

// Responses
export interface PedidoResponse {
  id: string;
  userId: string;
  productos: PedidoProductoResponse[];
  estado: EstadoPedido;
  comentario: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface PedidoProductoResponse {
  nombreProducto: string;
  cantidad: number;
}
