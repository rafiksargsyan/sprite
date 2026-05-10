#!/bin/sh
set -e

BUILD_TS=$(cat /usr/share/nginx/html/.build-ts)

cat > /usr/share/nginx/html/env.${BUILD_TS}.js <<EOF
Object.defineProperty(window, '__SPRITE_ENV__', {
  value: {
    VITE_API_BASE_URL: "${VITE_API_BASE_URL}",
    VITE_FIREBASE_API_KEY: "${VITE_FIREBASE_API_KEY}",
    VITE_FIREBASE_AUTH_DOMAIN: "${VITE_FIREBASE_AUTH_DOMAIN}",
    VITE_FIREBASE_PROJECT_ID: "${VITE_FIREBASE_PROJECT_ID}",
    VITE_FIREBASE_STORAGE_BUCKET: "${VITE_FIREBASE_STORAGE_BUCKET}",
    VITE_FIREBASE_MESSAGING_SENDER_ID: "${VITE_FIREBASE_MESSAGING_SENDER_ID}",
    VITE_FIREBASE_APP_ID: "${VITE_FIREBASE_APP_ID}"
  },
  writable: false,
  configurable: false
});
EOF

exec nginx -g 'daemon off;'
