import { ActivatedRoute, Router } from '@angular/router';

/**
 * Lee los query params actuales como record plano (para inicializar signals
 * de filtros y así "guardar" filtros en la ruta, compartible por URL).
 */
export function leerFiltrosDeRuta(route: ActivatedRoute): Record<string, string> {
  const salida: Record<string, string> = {};
  const mapa = route.snapshot.queryParamMap;

  for (const clave of mapa.keys) {
    const valor = mapa.get(clave);
    if (valor !== null && valor !== '') salida[clave] = valor;
  }

  return salida;
}

/**
 * Quita el prefijo de operador de la librería (`ilike:`, `gte:`, …) para
 * mostrar texto limpio en los inputs al restaurar filtros desde la URL.
 */
export function extraerTexto(valor: string | null | undefined): string {
  if (!valor) return '';
  const corte = valor.search(/[:|]/);
  return corte < 0 ? valor : valor.slice(corte + 1);
}

/**
 * Escribe los filtros en la URL (merge + replaceUrl: no ensucia el historial).
 * Llamar solo cuando el serializado cambió para evitar loops de navegación.
 */
export function escribirFiltrosEnRuta(
  router: Router,
  route: ActivatedRoute,
  filtros: Record<string, string | number>,
): void {
  const limpio: Record<string, string> = {};

  for (const [clave, valor] of Object.entries(filtros)) {
    const texto = String(valor ?? '').trim();
    if (texto !== '') limpio[clave] = texto;
  }

  void router.navigate([], {
    relativeTo: route,
    queryParams: limpio,
    queryParamsHandling: '',
    replaceUrl: true,
  });
}
