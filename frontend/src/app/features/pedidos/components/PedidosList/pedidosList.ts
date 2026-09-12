import { Component, inject, signal } from "@angular/core";
import { PedidosService } from "../../core/pedidos.service";
import { PedidosListCard } from "./components/pedidosListCard/pedidosListCard";
import { lastValueFrom } from "rxjs";
import { injectQuery } from "@tanstack/angular-query-experimental";

@Component({
    selector: 'app-pedidos-list',
    standalone: true,
    imports: [PedidosListCard],
    templateUrl: './pedidosList.html',
    styleUrl: './pedidosList.css'
})
export class PedidosList
{
    private readonly pedidoService = inject(PedidosService);

    readonly cargando = signal(true);

    readonly pedidosQuery = injectQuery(() => (
    {
        queryKey: ['pedidos'],
        queryFn: async () =>
        {
            const pedidos = await lastValueFrom(this.pedidoService.obtenerPedidos());

            return pedidos.sort((a, b) => new Date(b.createdAt).getTime() -new Date(a.createdAt).getTime());
        }
    }));
}