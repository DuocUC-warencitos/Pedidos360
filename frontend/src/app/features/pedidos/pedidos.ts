import { Component } from "@angular/core";
import { RouterLink, RouterOutlet } from "@angular/router";

@Component({
	selector: 'app-pedidos',
	standalone: true,
	imports: [RouterOutlet, RouterLink],
	templateUrl: './pedidos.html',
	styleUrl: './pedidos.css'
})
export class Pedidos
{

}