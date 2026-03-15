import React from 'react';
import { ThemeProvider, createTheme } from '@mui/material/styles';
import CssBaseline from '@mui/material/CssBaseline';
import { Box, Typography, Button, Card, CardContent, Avatar } from '@mui/material';
import { styled } from '@mui/material/styles';
import { signOut } from 'firebase/auth';
import LogoutIcon from '@mui/icons-material/Logout';
import { useAuth } from '../hooks/useAuth';
import { auth } from '../lib/firebase';
import Loading from '../components/Loading/Loading';
import Login from '../pages/Login';

const theme = createTheme({
  palette: {
    primary: {
      main: '#667eea',
    },
    secondary: {
      main: '#764ba2',
    },
  },
  typography: {
    fontFamily: [
      '-apple-system',
      'BlinkMacSystemFont',
      '"Segoe UI"',
      'Roboto',
      '"Helvetica Neue"',
      'Arial',
      'sans-serif',
    ].join(','),
  },
});

const DashboardContainer = styled(Box)(({ theme }) => ({
  minHeight: '100vh',
  background: 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)',
  padding: theme.spacing(4),
  display: 'flex',
  alignItems: 'center',
  justifyContent: 'center',
}));

const ProfileCard = styled(Card)(({ theme }) => ({
  maxWidth: '500px',
  width: '100%',
  borderRadius: theme.spacing(2),
  boxShadow: '0 10px 40px rgba(0, 0, 0, 0.2)',
}));

const App: React.FC = () => {
  const { user, loading } = useAuth();

  const handleSignOut = async () => {
    try {
      await signOut(auth);
    } catch (error) {
      console.error('Error signing out:', error);
    }
  };

  if (loading) {
    return (
      <ThemeProvider theme={theme}>
        <CssBaseline />
        <Loading />
      </ThemeProvider>
    );
  }

  if (!user) {
    return (
      <ThemeProvider theme={theme}>
        <CssBaseline />
        <Login />
      </ThemeProvider>
    );
  }

  return (
    <ThemeProvider theme={theme}>
      <CssBaseline />
      <DashboardContainer>
        <ProfileCard>
          <CardContent sx={{ p: 4, textAlign: 'center' }}>
            <Avatar
              src={user.photoURL || undefined}
              sx={{ 
                width: 100, 
                height: 100, 
                margin: '0 auto 24px',
                fontSize: '48px',
                background: theme.palette.primary.main,
              }}
            >
              {user.email?.charAt(0).toUpperCase()}
            </Avatar>

            <Typography variant="h4" gutterBottom fontWeight={700}>
              Welcome!
            </Typography>

            <Box sx={{ my: 3, textAlign: 'left' }}>
              <Box sx={{ mb: 2 }}>
                <Typography variant="caption" color="text.secondary">
                  Email
                </Typography>
                <Typography variant="body1" fontWeight={500}>
                  {user.email}
                </Typography>
              </Box>

              <Box sx={{ mb: 2 }}>
                <Typography variant="caption" color="text.secondary">
                  User ID
                </Typography>
                <Typography 
                  variant="body2" 
                  fontFamily="monospace"
                  sx={{ wordBreak: 'break-all' }}
                >
                  {user.uid}
                </Typography>
              </Box>

              <Box sx={{ mb: 2 }}>
                <Typography variant="caption" color="text.secondary">
                  Email Verified
                </Typography>
                <Typography variant="body1">
                  {user.emailVerified ? '✅ Yes' : '❌ No'}
                </Typography>
              </Box>

              {user.displayName && (
                <Box sx={{ mb: 2 }}>
                  <Typography variant="caption" color="text.secondary">
                    Display Name
                  </Typography>
                  <Typography variant="body1" fontWeight={500}>
                    {user.displayName}
                  </Typography>
                </Box>
              )}

              <Box>
                <Typography variant="caption" color="text.secondary">
                  Sign-in Provider
                </Typography>
                <Typography variant="body1" fontWeight={500}>
                  {user.providerData[0]?.providerId || 'Unknown'}
                </Typography>
              </Box>
            </Box>

            <Button
              variant="contained"
              color="primary"
              fullWidth
              onClick={handleSignOut}
              startIcon={<LogoutIcon />}
              sx={{ 
                mt: 2,
                py: 1.5,
                textTransform: 'none',
                fontSize: '16px',
                fontWeight: 600,
              }}
            >
              Sign Out
            </Button>
          </CardContent>
        </ProfileCard>
      </DashboardContainer>
    </ThemeProvider>
  );
};

export default App;