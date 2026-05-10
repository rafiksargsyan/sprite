import { defineConfig, type Plugin } from 'vite'
import react from '@vitejs/plugin-react'

function envScriptPlugin(): Plugin {
  const buildTs = process.env.BUILD_TS;
  return {
    name: 'env-script',
    apply: 'build',
    transformIndexHtml(html) {
      if (!buildTs) return html;
      return html.replace(
        '<head>',
        `<head>\n    <script src="/env.${buildTs}.js"></script>`,
      );
    },
  };
}

// https://vite.dev/config/
export default defineConfig({
  plugins: [
    react({
      babel: {
        plugins: [['babel-plugin-react-compiler']],
      },
    }),
    envScriptPlugin(),
  ],
})
