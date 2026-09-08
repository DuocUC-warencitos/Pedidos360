import { Component, OnInit } from '@angular/core';
import { RouterLink, RouterOutlet } from '@angular/router';
import { MsalService } from '@azure/msal-angular';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet, RouterLink],
  templateUrl: './app.html',
  styleUrl: './app.css',
})
export class App implements OnInit {

  constructor(private readonly msalService: MsalService) {}

  ngOnInit(): void {
    this.msalService
      .handleRedirectObservable({ navigateToLoginRequestUrl: true })
      .subscribe();
  }
}