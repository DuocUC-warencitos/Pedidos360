import { Component, input } from '@angular/core';

export type ButtonVariant =
  | 'primary'
  | 'secondary'
  | 'ghost'
  | 'danger'
  | 'success'
  | 'danger-ghost';

@Component({
  selector: 'ui-button',
  standalone: true,
  template: `
    <button
      [type]="type()"
      [disabled]="disabled()"
      [class]="buttonClass()"
    >
      <ng-content />
    </button>
  `,
})
export class UiButton {
  readonly variant = input<ButtonVariant>('primary');
  readonly type = input<'button' | 'submit'>('button');
  readonly disabled = input<boolean>(false);
  readonly fullWidth = input<boolean>(false);

  buttonClass(): string {
    const base =
      'inline-flex items-center justify-center gap-2 px-4 py-2.5 rounded-md text-[13px] font-bold border transition disabled:opacity-60 disabled:cursor-not-allowed disabled:transform-none disabled:shadow-none';
    const width = this.fullWidth() ? ' w-full' : '';
    const variants: Record<ButtonVariant, string> = {
      primary: 'bg-[var(--color-primary-dark)] text-white border-[var(--color-primary-dark)] hover:bg-[#111827]',
      secondary: 'bg-[#5d6874] text-white border-transparent hover:brightness-110',
      ghost: 'bg-white text-[var(--color-text)] border-[#d1d5db] hover:bg-[#f3f4f6]',
      danger: 'bg-white text-[#b3261e] border-[#f5c6c3] hover:bg-[#fdecec]',
      'danger-ghost': 'bg-[#fff5f5] text-[#b91c1c] border-[#fecaca] hover:bg-[#fee2e2]',
      success: 'bg-[#1f7a3f] text-white border-[#1a6433] hover:bg-[#1a6433]',
    };
    return `${base} ${variants[this.variant()]}${width}`;
  }
}
