import {
  Component,
  computed,
  input,
  output,
} from '@angular/core';
import { RouterLink } from '@angular/router';

import { ProductoResponse } from '@features/productos/data/productos.types';

@Component({
  selector: 'app-producto-card',
  standalone: true,
  imports: [RouterLink],
  templateUrl: './producto-card.html',
})
export class ProductoCard {
  readonly producto =
    input.required<ProductoResponse>();

  readonly eliminar = output<void>();

  readonly idCorto = computed(() =>
    this.producto().id.slice(0, 8),
  );
}
