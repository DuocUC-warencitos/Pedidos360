import { Component, inject, signal } from "@angular/core";
import { PedidosService } from "../../core/pedidos.service";
import { PedidosListCard } from "./components/pedidosListCard/pedidosListCard";
import { lastValueFrom } from "rxjs";
import { injectMutation, injectQuery, QueryClient } from "@tanstack/angular-query-experimental";

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
    private readonly queryClient = inject(QueryClient);

    readonly pedidosQuery = injectQuery(() => (
    {
        queryKey: ['pedidos'],
        queryFn: async () =>
        {
            const pedidos = await lastValueFrom(this.pedidoService.obtenerPedidos());

            return pedidos.sort((a, b) => new Date(b.createdAt).getTime() -new Date(a.createdAt).getTime());
        }
    }));

    readonly eliminarTodosMutation = injectMutation(() => (
    {
        mutationFn: async () =>
            await lastValueFrom(this.pedidoService.eliminarTodosLosPedidos()),

        onSuccess: () =>
        {
            this.queryClient.invalidateQueries({ queryKey: ['pedidos'] });
        }
    }));
}