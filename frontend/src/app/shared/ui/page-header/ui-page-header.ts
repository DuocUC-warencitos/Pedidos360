import { Component, input } from '@angular/core';

@Component({
  selector: 'ui-page-header',
  standalone: true,
  template: `
    <header class="mb-7">
      @if (eyebrow()) {
        <span
          class="block text-[11px] font-bold tracking-[1.5px] text-[#6b7280]"
          >{{ eyebrow() }}</span
        >
      }
      <h1 class="mt-1 mb-2 text-[32px] font-bold tracking-tight">
        {{ title() }}
      </h1>
      @if (description()) {
        <p class="m-0 text-[#6b7280] leading-relaxed text-sm">
          {{ description() }}
        </p>
      }
      <ng-content />
    </header>
  `,
})
export class UiPageHeader {
  readonly eyebrow = input<string>('');
  readonly title = input.required<string>();
  readonly description = input<string>('');
}
