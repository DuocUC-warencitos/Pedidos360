import { Component, inject } from "@angular/core";
import { PedidosService } from "../../core/pedidos.service";
import { toSignal } from "@angular/core/rxjs-interop";
import { PedidosListCard } from "./components/pedidosListCard/pedidosListCard";

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

    readonly pedidos = toSignal(this.pedidoService.obtenerPedidos(), 
    { 
        initialValue: [] 
    });
}