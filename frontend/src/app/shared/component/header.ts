import { Component, Signal } from "@angular/core";
import { RouterLink } from "@angular/router";
import { AuthService } from "../../core/auth/auth.service";
import { AuthUser } from "../../core/auth/authenticationProvider";

@Component({
  selector: 'app-header',
  standalone: true,
  imports: [RouterLink],
  templateUrl: './header.html',
  styleUrl: './header.css'
})
export class HeaderComponent
{
  constructor(private readonly authService: AuthService)
  {
  }

  get user(): Signal<AuthUser | null> 
  {
      return this.authService.user;
  }

  login(): void
  {
    this.authService.login();
  }

  logout(): void
  {
    this.authService.logout();
  }
}