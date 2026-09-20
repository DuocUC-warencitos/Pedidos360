// Requests

export interface ProductoRequest {
  nombre: string;
  precio: number;
  stock: number;
}

// Responses

export interface ProductoResponse 
{
  id: string;
  nombre: string;
  precio: number;
  stockDisponible: number;
}

export interface ProductoDetailResponse 
{
  id: string;
  nombre: string;
  precio: number;
  stockDisponible: number;
  stockReservado: number;
}

