import { Component } from '@angular/core';

@Component({
  selector: 'ui-estado-leyenda',
  standalone: true,
  template: `
    <div
      class="flex flex-wrap items-center gap-4 text-xs text-[#6b7280]"
    >
      <span class="inline-flex items-center gap-1.5 cursor-default select-none">
        <span
          class="inline-block w-2.5 h-2.5 rounded-full bg-[#9ca3af] shrink-0 transition hover:scale-125"
        ></span>
        Creado
      </span>
      <span class="inline-flex items-center gap-1.5 cursor-default select-none">
        <span
          class="inline-block w-2.5 h-2.5 rounded-full bg-[#1f7a3f] shrink-0"
        ></span>
        Confirmado
      </span>
      <span class="inline-flex items-center gap-1.5 cursor-default select-none">
        <span
          class="inline-block w-2.5 h-2.5 rounded-full bg-[#d99a00] shrink-0"
        ></span>
        En preparación
      </span>
      <span class="inline-flex items-center gap-1.5 cursor-default select-none">
        <span
          class="inline-block w-2.5 h-2.5 rounded-full bg-[#1a56b8] shrink-0"
        ></span>
        Despachado
      </span>
      <span class="inline-flex items-center gap-1.5 cursor-default select-none">
        <span
          class="inline-block w-2.5 h-2.5 rounded-full bg-[#1f7a3f] shrink-0"
        ></span>
        Entregado
      </span>
      <span class="inline-flex items-center gap-1.5 cursor-default select-none">
        <span
          class="inline-block w-2.5 h-2.5 rounded-full bg-[#b3261e] shrink-0"
        ></span>
        Cancelado
      </span>
    </div>
  `,
})
export class UiEstadoLeyenda {}
