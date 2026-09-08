import { Component } from '@angular/core';

@Component({
  selector: 'app-protegido',
  standalone: true,
  template: `
    <section class="tarjeta protegida">
      <h2>Área protegida</h2>
      <p>
        Si puedes ver esta página, MsalGuard permitió el acceso.
      </p>
      <p>
        El siguiente paso es consumir la API Spring Boot utilizando
        un Access Token con el scope <strong>Pedidos.Read</strong>.
      </p>
    </section>
  `
})

export class Protegido {}
