import { Component } from '@angular/core';

@Component({
  selector: 'app-protegido',
  standalone: true,
  template: `
    <section class="bg-white border border-[#d5dbe3] rounded-xl p-6 mb-5">
      <h2 class="text-[#e66c00] font-bold text-xl mb-3">Área protegida</h2>
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
