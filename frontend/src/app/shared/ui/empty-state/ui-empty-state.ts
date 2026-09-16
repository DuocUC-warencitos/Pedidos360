import { Component, input } from '@angular/core';

@Component({
  selector: 'ui-empty-state',
  standalone: true,
  template: `
    <div
      class="flex flex-col items-center text-center p-9 border border-dashed border-[#d1d5db] rounded-lg"
    >
      <div
        class="flex items-center justify-center w-10 h-10 mb-3 rounded-full bg-[#f1f3f5] text-[#6b7280] text-2xl"
      >
        {{ icon() }}
      </div>
      <strong class="text-sm">{{ title() }}</strong>
      @if (description()) {
        <p class="mt-1.5 text-xs text-[#6b7280]">{{ description() }}</p>
      }
      <ng-content />
    </div>
  `,
})
export class UiEmptyState {
  readonly icon = input<string>('+');
  readonly title = input.required<string>();
  readonly description = input<string>('');
}
