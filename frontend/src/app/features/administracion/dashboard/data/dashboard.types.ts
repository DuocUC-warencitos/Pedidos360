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
