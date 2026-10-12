import { Component } from "@angular/core";
import { RouterLink, RouterLinkActive, RouterOutlet } from "@angular/router";

@Component(
{
    selector: 'app-admin-page',
    standalone: true,
    imports: [RouterLinkActive, RouterLink, RouterOutlet],
    templateUrl: './admin-page.html'
})
export class AdminPage
{

}