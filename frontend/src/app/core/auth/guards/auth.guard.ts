import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { MsalService } from '@azure/msal-angular';
import { AccountInfo } from '@azure/msal-browser';

import { AuthService } from '../auth.service';

export const authGuard: CanActivateFn = () => {
  const auth = inject(AuthService);
  const msal = inject(MsalService);
  const router = inject(Router);

  if (auth.isAuthenticated) return true;

  const account: AccountInfo | null = msal.instance.getActiveAccount() ?? msal.instance.getAllAccounts()[0] ?? null;
  if (account) {
    msal.instance.setActiveAccount(account);
    // re-evaluate after setting active account
    if (auth.isAuthenticated) return true;
  }

  // No user -> redirect to home (login is via header) or show unauthorized
  router.navigateByUrl('/');
  return false;
};
