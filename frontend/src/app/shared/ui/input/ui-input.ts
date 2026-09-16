import { Component, input, model } from '@angular/core';
import { FormsModule } from '@angular/forms';

@Component({
  selector: 'ui-input',
  standalone: true,
  imports: [FormsModule],
  template: `
    <div class="flex flex-col gap-1.5">
      <label
        [for]="id()"
        class="text-[13px] font-bold"
        >{{ label() }}</label
      >
      <input
        [id]="id()"
        [type]="type()"
        [placeholder]="placeholder()"
        [min]="min()"
        [(ngModel)]="value"
        class="w-full px-3 py-2.5 border border-[#d1d5db] rounded-md bg-white text-[#1f2937] text-sm font-mono placeholder:text-[#9ca3af] focus:outline-none focus:border-[#1f2937] focus:ring-2 focus:ring-[rgba(31,41,55,0.08)] transition"
      />
    </div>
  `,
})
export class UiInput {
  readonly id = input.required<string>();
  readonly label = input.required<string>();
  readonly type = input<string>('text');
  readonly placeholder = input<string>('');
  readonly min = input<string | number | undefined>(undefined);
  readonly value = model.required<string | number>();
}
