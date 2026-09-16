import { Component, input } from '@angular/core';

@Component({
  selector: 'ui-card',
  standalone: true,
  template: `
    <div [class]="cardClass()">
      <ng-content />
    </div>
  `,
})
export class UiCard {
  readonly padding = input<string>('p-6');
  readonly hover = input<boolean>(false);

  cardClass(): string {
    const base =
      'bg-white border border-[var(--color-border)] rounded-xl shadow-sm';
    const hover = this.hover() ? ' transition hover:-translate-y-1 hover:shadow-lg hover:border-[#c7ced8]' : '';
    return `${base} ${this.padding()}${hover}`;
  }
}
