import { HttpParams } from '@angular/common/http';

/** Construye HttpParams omitiendo valores vacíos; los arrays se repiten (?sort=a&sort=b). */
export function construirParams(
  filtros: Record<string, string | number | string[] | undefined>,
): HttpParams {
  let params = new HttpParams();

  for (const [clave, valor] of Object.entries(filtros)) {
    if (valor === undefined || valor === null) continue;

    if (Array.isArray(valor)) {
      for (const item of valor) {
        const texto = String(item ?? '').trim();
        if (texto !== '') params = params.append(clave, texto);
      }
      continue;
    }

    const texto = String(valor).trim();
    if (texto !== '') params = params.set(clave, texto);
  }

  return params;
}
