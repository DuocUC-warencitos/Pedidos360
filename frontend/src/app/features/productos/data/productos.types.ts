// Requests

export interface ProductoRequest
{
    nombre: string;
    precio: number;
    stock: number;
    descripcion?: string;
    shortDescription?: string;
    sku?: string;
    categoria?: string;
    stockMinimo?: number | null;
    activo?: boolean | null;
}

// Filtros (contrato roony-specification-query-params + page/size/sort)

export interface ProductoFiltros {
  q?: string;
  cat?: string;
  categoria?: string;
  sku?: string;
  desde?: string;
  hasta?: string;
  activo?: string;
  page?: number;
  size?: number;
  sort?: string | string[];
}

// Responses

export interface ProductoStatsResponse {
  total: number;
  activos: number;
  stockTotal: number;
  stockReservadoTotal?: number;
}

export interface ProductoResponse
{
    id: string;
    nombre: string;
    precio: number;
    stockDisponible: number;
    stockReservado?: number;
    sku?: string;
    shortDescription?: string;
    activo?: boolean;
}

export interface ProductoDetailResponse
{
    id: string;
    nombre: string;
    precio: number;
    stockDisponible: number;
    stockReservado: number;
    descripcion?: string;
    shortDescription?: string;
    sku?: string;
    categoria?: string;
    stockMinimo?: number | null;
    activo?: boolean;
    createdAt?: string;
    updatedAt?: string;
}