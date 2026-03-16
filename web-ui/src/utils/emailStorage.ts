const EMAIL_KEY = 'sprite_signin_email';

export const saveEmailForSignIn = (email: string) => {
  localStorage.setItem(EMAIL_KEY, email);
};

export const getEmailForSignIn = (): string | null => {
  return localStorage.getItem(EMAIL_KEY);
};

export const clearEmailForSignIn = () => {
  localStorage.removeItem(EMAIL_KEY);
};
