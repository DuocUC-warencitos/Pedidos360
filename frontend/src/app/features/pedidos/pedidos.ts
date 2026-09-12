import { Component } from "@angular/core";
import { PedidosForm } from "./components/PedidosForm/pedidosForm";
import { PedidosList } from "./components/PedidosList/pedidosList";

@Component({
	selector: 'app-pedidos',
	standalone: true,
	imports: [PedidosForm, PedidosList],
	templateUrl: './pedidos.html',
	styleUrl: './pedidos.css'
})
export class Pedidos
{

}