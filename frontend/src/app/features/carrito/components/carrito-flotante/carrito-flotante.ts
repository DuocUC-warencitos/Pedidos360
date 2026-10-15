import { CurrencyPipe } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';

import { CarritoService } from '@features/carrito/data/carrito.service';
import { PedidoCheckoutService } from '@features/pedidos/data/pedido-checkout.service';
import { AuthService } from '@core/auth/auth.service';

@Component({
    selector: 'app-carrito-flotante',
    standalone: true,
    imports: [
        CurrencyPipe,
        RouterLink
    ],
    templateUrl: './carrito-flotante.html',
})
export class CarritoFlotante
{
    readonly carritoService = inject(CarritoService);
    readonly checkout = inject(PedidoCheckoutService);
    private readonly auth = inject(AuthService);

    readonly user = this.auth.user;

    readonly abierto = signal(false);

    togglePanel(): void
    {
        this.abierto.update(abierto => !abierto);
    }

    cerrarPanel(): void
    {
        this.abierto.set(false);
    }
}