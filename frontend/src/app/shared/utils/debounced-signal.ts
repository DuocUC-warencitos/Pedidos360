import { effect, Signal, signal } from '@angular/core';

/**
 * Deriva un signal con debounce: ideal para textos que se escriben, para no
 * disparar un request por cada tecla. Debe llamarse en contexto de inyección
 * (campo o constructor del componente).
 */
export function debouncedSignal(origen: Signal<string>, esperaMs = 400): Signal<string> {
  const actual = signal(origen());

  effect(
    (onCleanup) => {
      const valor = origen();
      const temporizador = setTimeout(() => actual.set(valor), esperaMs);
      onCleanup(() => clearTimeout(temporizador));
    },
    { allowSignalWrites: true },
  );

  return actual.asReadonly();
}
