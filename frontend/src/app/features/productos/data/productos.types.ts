// Requests

export interface ProductoRequest {
  nombre: string;
  precio: number;
  stock: number;
  descripcion?: string;
  sku?: string;
  categoria?: string;
  stockMinimo?: number;
  activo?: boolean;
}

// Responses

export interface ProductoResponse 
{
  id: string;
  nombre: string;
  precio: number;
  stockDisponible: number;
  sku?: string;
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
  sku?: string;
  categoria?: string;
  stockMinimo?: number;
  activo?: boolean;
}

