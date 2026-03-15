const EMAIL_KEY = 'emailForSignIn';

export const emailStorage = {
  save: (email) => {
    try {
      window.localStorage.setItem(EMAIL_KEY, email);
      return true;
    } catch (error) {
      console.error('Error saving email:', error);
      return false;
    }
  },

  get: () => {
    try {
      return window.localStorage.getItem(EMAIL_KEY);
    } catch (error) {
      console.error('Error getting email:', error);
      return null;
    }
  },

  remove: () => {
    try {
      window.localStorage.removeItem(EMAIL_KEY);
      return true;
    } catch (error) {
      console.error('Error removing email:', error);
      return false;
    }
  },

  exists: () => {
    return !!emailStorage.get();
  }
};