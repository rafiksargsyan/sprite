import { useState } from 'react';
import {
  Box,
  Card,
  CardContent,
  Typography,
  Button,
  TextField,
  Divider,
  Alert,
  CircularProgress,
} from '@mui/material';
import GoogleIcon from '@mui/icons-material/Google';
import GitHubIcon from '@mui/icons-material/GitHub';
import EmailIcon from '@mui/icons-material/Email';
import { Navigate } from 'react-router-dom';
import { useAuth } from '../hooks/useAuth';

export function Login() {
  const { user, signInWithGoogle, signInWithGithub, sendMagicLink } = useAuth();
  const [email, setEmail] = useState('');
  const [emailSent, setEmailSent] = useState(false);
  const [error, setError] = useState('');
  const [loadingGoogle, setLoadingGoogle] = useState(false);
  const [loadingGithub, setLoadingGithub] = useState(false);
  const [loadingEmail, setLoadingEmail] = useState(false);

  const anyLoading = loadingGoogle || loadingGithub || loadingEmail;

  if (user) return <Navigate to="/dashboard" replace />;

  const handleGoogle = async () => {
    setError('');
    setLoadingGoogle(true);
    try {
      await signInWithGoogle();
    } catch (err: unknown) {
      setError(err instanceof Error ? err.message : 'Google sign-in failed');
      setLoadingGoogle(false);
    }
  };

  const handleGithub = async () => {
    setError('');
    setLoadingGithub(true);
    try {
      await signInWithGithub();
    } catch (err: unknown) {
      setError(err instanceof Error ? err.message : 'GitHub sign-in failed');
      setLoadingGithub(false);
    }
  };

  const handleSendLink = async () => {
    if (!email) {
      setError('Please enter your email address');
      return;
    }
    setError('');
    setLoadingEmail(true);
    try {
      await sendMagicLink(email);
      setEmailSent(true);
    } catch (err: unknown) {
      setError(err instanceof Error ? err.message : 'Failed to send magic link');
    } finally {
      setLoadingEmail(false);
    }
  };

  return (
    <Box
      sx={{
        minHeight: '100vh',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        bgcolor: 'grey.50',
      }}
    >
      <Card sx={{ width: '100%', maxWidth: 420, mx: 2 }} elevation={2}>
        <CardContent sx={{ p: 4 }}>
          <Box sx={{ textAlign: 'center', mb: 4 }}>
            <Typography variant="h4" fontWeight="bold" color="primary">
              Sprite
            </Typography>
            <Typography variant="body2" color="text.secondary" sx={{ mt: 0.5 }}>
              WebVTT thumbnail generation for your videos
            </Typography>
          </Box>

          {error && (
            <Alert severity="error" sx={{ mb: 2 }}>
              {error}
            </Alert>
          )}

          <Button
            fullWidth
            variant="outlined"
            size="large"
            startIcon={loadingGoogle ? <CircularProgress size={18} /> : <GoogleIcon />}
            onClick={handleGoogle}
            disabled={anyLoading}
            sx={{ mb: 1.5 }}
          >
            Continue with Google
          </Button>

          <Button
            fullWidth
            variant="outlined"
            size="large"
            startIcon={loadingGithub ? <CircularProgress size={18} /> : <GitHubIcon />}
            onClick={handleGithub}
            disabled={anyLoading}
            sx={{ mb: 2 }}
          >
            Continue with GitHub
          </Button>

          <Divider sx={{ my: 2 }}>
            <Typography variant="body2" color="text.secondary">
              or continue with email
            </Typography>
          </Divider>

          {emailSent ? (
            <Alert severity="success">
              Magic link sent to <strong>{email}</strong>. Check your inbox!
            </Alert>
          ) : (
            <>
              <TextField
                fullWidth
                label="Email address"
                type="email"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                onKeyDown={(e) => e.key === 'Enter' && handleSendLink()}
                disabled={anyLoading}
                sx={{ mb: 1.5 }}
              />
              <Button
                fullWidth
                variant="contained"
                size="large"
                startIcon={
                  loadingEmail ? <CircularProgress size={18} color="inherit" /> : <EmailIcon />
                }
                onClick={handleSendLink}
                disabled={anyLoading}
              >
                Send magic link
              </Button>
            </>
          )}
        </CardContent>
      </Card>
    </Box>
  );
}
