import { Component, input, output } from '@angular/core';

@Component({
  selector: 'ui-pager',
  standalone: true,
  template: `
    <nav class="mt-6 flex flex-wrap items-center justify-between gap-3" aria-label="Paginación">
      <p class="m-0 text-xs text-muted">
        Página {{ paginaActual() + 1 }} de {{ totalPaginas() }} · {{ totalElementos() }}
        {{ totalElementos() === 1 ? 'resultado' : 'resultados' }}
      </p>
      <div class="flex flex-wrap items-center gap-2">
        <label class="flex items-center gap-2 text-xs font-semibold text-muted">
          Por página
          <select
            [value]="tamanoPagina()"
            (change)="alCambiarTamano($event)"
            class="rounded-lg border border-border bg-white px-2 py-2 text-sm text-[#1f2937] outline-none transition focus:border-[#64748b]"
          >
            @for (opcion of opcionesTamano(); track opcion) {
              <option [value]="opcion">{{ opcion }}</option>
            }
          </select>
        </label>
        <button
          type="button"
          (click)="anterior.emit()"
          [disabled]="paginaActual() <= 0"
          class="rounded-lg border border-border bg-white px-4 py-2 text-sm font-semibold text-[#1f2937] transition hover:bg-surface-secondary disabled:cursor-not-allowed disabled:opacity-50"
        >
          ← Anterior
        </button>
        <button
          type="button"
          (click)="siguiente.emit()"
          [disabled]="paginaActual() >= totalPaginas() - 1"
          class="rounded-lg border border-border bg-white px-4 py-2 text-sm font-semibold text-[#1f2937] transition hover:bg-surface-secondary disabled:cursor-not-allowed disabled:opacity-50"
        >
          Siguiente →
        </button>
      </div>
    </nav>
  `,
})
export class UiPager {
  /** Página actual base 0. */
  readonly paginaActual = input.required<number>();
  readonly totalPaginas = input.required<number>();
  readonly totalElementos = input.required<number>();
  /** Tamaño de página actual. */
  readonly tamanoPagina = input<number>(8);
  readonly opcionesTamano = input<number[]>([8, 12, 20, 50]);

  readonly anterior = output<void>();
  readonly siguiente = output<void>();
  readonly tamanoChange = output<number>();

  alCambiarTamano(evento: Event): void {
    const valor = Number((evento.target as HTMLSelectElement | null)?.value);
    if (Number.isInteger(valor) && valor > 0) this.tamanoChange.emit(valor);
  }
}
