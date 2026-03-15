import React, { useState } from 'react';
import {
  Box,
  TextField,
  Typography,
  Alert,
  CircularProgress,
} from '@mui/material';
import { styled } from '@mui/material/styles';
import { signInWithEmailLink } from 'firebase/auth';
import { auth } from '../../lib/firebase';
import ProviderButton from '../ProviderButton/ProviderButton';
import EmailIcon from '@mui/icons-material/Email';

const ConfirmationContainer = styled(Box)(({ theme }) => ({
  display: 'flex',
  flexDirection: 'column',
  gap: theme.spacing(3),
  width: '100%',
}));

const IconWrapper = styled(Box)(({ theme }) => ({
  width: '80px',
  height: '80px',
  margin: '0 auto',
  display: 'flex',
  alignItems: 'center',
  justifyContent: 'center',
  background: theme.palette.primary.main,
  borderRadius: '50%',
  marginBottom: theme.spacing(2),
}));

interface EmailConfirmationProps {
  onSuccess: () => void;
}

const EmailConfirmation: React.FC<EmailConfirmationProps> = ({ onSuccess }) => {
  const [email, setEmail] = useState<string>('');
  const [loading, setLoading] = useState<boolean>(false);
  const [error, setError] = useState<string>('');

  const handleConfirmEmail = async () => {
    if (!email.trim()) {
      setError('Please enter your email address');
      return;
    }

    setLoading(true);
    setError('');

    try {
      await signInWithEmailLink(auth, email, window.location.href);
      
      // Clear the URL
      window.history.replaceState({}, document.title, window.location.pathname);
      
      // Clear localStorage
      window.localStorage.removeItem('emailForSignIn');
      
      onSuccess();
    } catch (err: any) {
      console.error('Error confirming email:', err);
      setError(err.message || 'Failed to confirm email. Please try again.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <ConfirmationContainer>
      <Box textAlign="center">
        <IconWrapper>
          <EmailIcon sx={{ fontSize: 40, color: 'white' }} />
        </IconWrapper>
        <Typography variant="h5" gutterBottom fontWeight={600}>
          Confirm Your Email
        </Typography>
        <Typography variant="body2" color="text.secondary">
          Please enter your email to complete sign-in
        </Typography>
      </Box>

      {error && (
        <Alert severity="error" onClose={() => setError('')}>
          {error}
        </Alert>
      )}

      <TextField
        fullWidth
        label="Email Address"
        type="email"
        value={email}
        onChange={(e) => setEmail(e.target.value)}
        onKeyPress={(e) => e.key === 'Enter' && handleConfirmEmail()}
        disabled={loading}
        autoFocus
        variant="outlined"
      />

      <ProviderButton
        provider="email"
        onClick={handleConfirmEmail}
        disabled={loading}
        icon={loading ? <CircularProgress size={20} color="inherit" /> : null}
      >
        {loading ? 'Confirming...' : 'Confirm Email'}
      </ProviderButton>
    </ConfirmationContainer>
  );
};

export default EmailConfirmation;