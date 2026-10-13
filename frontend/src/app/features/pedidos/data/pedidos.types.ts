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
  | 'ACEPTADO'
  | 'CONFIRMADO'
  | 'EN_PREPARACION'
  | 'DESPACHADO'
  | 'ENTREGADO'
  | 'CANCELADO';

// Filtros (contrato roony-specification-query-params + page/size/sort)

export interface PedidoFiltros {
  estado?: string;
  page?: number;
  size?: number;
  sort?: string | string[];
}

export interface PedidoAdminFiltros extends PedidoFiltros {
  usuario?: string;
}

// Responses
export interface PedidoStatsResponse {
  total: number;
  cancelables: number;
  cancelados: number;
}

export interface PedidoResponse {
  id: string;
  userId: string;
  productos: PedidoProductoResponse[];
  estado: EstadoPedido;
}

export interface PedidoDetailResponse {
  id: string;
  userId: string;
  productos: PedidoProductoResponse[];
  estado: EstadoPedido;
  comentario: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface PedidoProductoResponse {
  productoId: string;
  nombreProducto: string;
  cantidad: number;
  precio: number;
}
