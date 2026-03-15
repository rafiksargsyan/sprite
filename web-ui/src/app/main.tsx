import { createRoot } from 'react-dom/client'
import App from './App.tsx'
import { createTheme, ThemeProvider } from '@mui/material/styles'
import CssBaseline from '@mui/material/CssBaseline'
// import { StrictMode } from 'react'
import { green } from '@mui/material/colors'

const theme = createTheme({
  components: {
    MuiButton: {
      defaultProps: {
        disableElevation: true,
      },
    },
    MuiPaper: {
      defaultProps: { 
        elevation: 0,
      },
    },
  },
  palette: {
    mode: 'light', 
    primary: {
      main: green[700],
    },
  },
});

createRoot(document.getElementById('root')!).render(
  //  <StrictMode>
    <ThemeProvider theme={theme}>
      <CssBaseline />
      <App />
    </ThemeProvider>
  //  </StrictMode>
)
