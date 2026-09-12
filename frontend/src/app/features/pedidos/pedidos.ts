import { Component } from "@angular/core";
import { PedidosForm } from "./components/PedidosForm/pedidosForm";

@Component({
	selector: 'app-pedidos',
	standalone: true,
	imports: [PedidosForm],
	templateUrl: './pedidos.html',
	styleUrl: './pedidos.css'
})
export class Pedidos
{

}