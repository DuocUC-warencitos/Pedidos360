import { Component, inject } from "@angular/core";
import { PedidosService } from "../../core/pedidos.service";
import { toSignal } from "@angular/core/rxjs-interop";

@Component({
    selector: 'app-pedidos-list',
    standalone: true,
    imports: [],
    templateUrl: './pedidosList.html',
    styleUrl: './pedidosList.css'
})
export class PedidosList
{
    private readonly pedidoService = inject(PedidosService);

    readonly pedidos = toSignal(this.pedidoService.obtenerPedidos(), { initialValue: [] });
}