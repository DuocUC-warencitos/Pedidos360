import { Component, input, output } from "@angular/core";
import { PedidoResponse } from "../../../../pedidos.type";
import { DatePipe } from "@angular/common";

@Component({
    selector: 'app-pedido-card',
    standalone: true,
    imports: [DatePipe],
    templateUrl: './pedidosListCard.html',
    styleUrl: './pedidosListCard.css'
})
export class PedidosListCard
{
    readonly pedido = input.required<PedidoResponse>();

    avanzar  = output<void>();
    cancelar = output<void>();
}