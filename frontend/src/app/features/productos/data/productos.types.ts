// Requests

export interface ProductoRequest {
  nombre: string;
  precio: number;
  stock: number;
}

// Responses

export interface ProductoResponse {
  id: string;
  nombre: string;
  precio: number;
  stockDisponible: number;
}
