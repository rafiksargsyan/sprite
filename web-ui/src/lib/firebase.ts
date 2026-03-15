import { initializeApp } from 'firebase/app';
import { getAuth } from 'firebase/auth';

const firebaseConfig = {
  apiKey: "AIzaSyA6paHHtkH-vHHHoSeRAv84BTMvi59YtJo",
  authDomain: "sprite-6193a.firebaseapp.com",
  projectId: "sprite-6193a",
  appId: "1:414480725021:web:40cef0adcfd29c352b02f4"
};

const app = initializeApp(firebaseConfig);
export const auth = getAuth(app);
export default app;
