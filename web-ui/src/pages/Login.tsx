import React, { useState, useEffect } from 'react';
import {
  Box,
  Card,
  CardContent,
  TextField,
  Typography,
  Divider,
  Alert,
  CircularProgress,
  Fade,
} from '@mui/material';
import { styled } from '@mui/material/styles';
import {
  signInWithEmailLink,
  sendSignInLinkToEmail,
  signInWithPopup,
  GoogleAuthProvider,
  GithubAuthProvider,
  isSignInWithEmailLink,
} from 'firebase/auth';
import { auth } from '../lib/firebase';
import EmailIcon from '@mui/icons-material/Email';
import GoogleIcon from '@mui/icons-material/Google';
import GitHubIcon from '@mui/icons-material/GitHub';
import CheckCircleIcon from '@mui/icons-material/CheckCircle';
import type { LoginPageProps } from '../types/auth.types';
import EmailConfirmation from '../components/EmailConfirmation/EmailConfirmation';
import ProviderButton from '../components/ProviderButton/ProviderButton';

const PageContainer = styled(Box)(({ theme }) => ({
  minHeight: '100vh',
  display: 'flex',
  alignItems: 'center',
  justifyContent: 'center',
  background: 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)',
  padding: theme.spacing(2),
}));

const StyledCard = styled(Card)(({ theme }) => ({
  maxWidth: '480px',
  width: '100%',
  borderRadius: theme.spacing(2),
  boxShadow: '0 10px 40px rgba(0, 0, 0, 0.2)',
}));

const Header = styled(Box)(({ theme }) => ({
  textAlign: 'center',
  marginBottom: theme.spacing(4),
}));

const ProviderSection = styled(Box)(({ theme }) => ({
  display: 'flex',
  flexDirection: 'column',
  gap: theme.spacing(2),
}));

const DividerWithText = styled(Box)(({ theme }) => ({
  display: 'flex',
  alignItems: 'center',
  gap: theme.spacing(2),
  margin: theme.spacing(3, 0),
}));

const EmailSentContainer = styled(Box)(({ theme }) => ({
  textAlign: 'center',
  padding: theme.spacing(3),
}));

const SuccessIconWrapper = styled(Box)(({ theme }) => ({
  width: '80px',
  height: '80px',
  margin: '0 auto',
  display: 'flex',
  alignItems: 'center',
  justifyContent: 'center',
  background: theme.palette.success.main,
  borderRadius: '50%',
  marginBottom: theme.spacing(2),
}));

const Login: React.FC<LoginPageProps> = ({ onLoginSuccess }) => {
  const [email, setEmail] = useState<string>('');
  const [loading, setLoading] = useState<boolean>(false);
  const [error, setError] = useState<string>('');
  const [emailSent, setEmailSent] = useState<boolean>(false);
  const [showEmailConfirmation, setShowEmailConfirmation] = useState<boolean>(false);
  const [processingEmailLink, setProcessingEmailLink] = useState<boolean>(false);

  useEffect(() => {
    // Check if this is an email link sign-in
    if (isSignInWithEmailLink(auth, window.location.href)) {
      handleEmailLinkSignIn();
    }
  }, []);

  const handleEmailLinkSignIn = async () => {
    setProcessingEmailLink(true);
    
    // Try to get email from localStorage (same device)
    let emailFromStorage = window.localStorage.getItem('emailForSignIn');
    
    if (emailFromStorage) {
      // Same device - auto-confirm
      try {
        await signInWithEmailLink(auth, emailFromStorage, window.location.href);
        
        // Clean up
        window.localStorage.removeItem('emailForSignIn');
        window.history.replaceState({}, document.title, window.location.pathname);
        
        setProcessingEmailLink(false);
        
        if (onLoginSuccess && auth.currentUser) {
          onLoginSuccess(auth.currentUser);
        }
      } catch (err: any) {
        console.error('Error with auto email confirmation:', err);
        // If auto-confirm fails, show manual confirmation
        setShowEmailConfirmation(true);
        setProcessingEmailLink(false);
      }
    } else {
      // Different device - show confirmation screen
      setShowEmailConfirmation(true);
      setProcessingEmailLink(false);
    }
  };

  const handleEmailLinkAuth = async (e: React.FormEvent) => {
    e.preventDefault();
    
    if (!email.trim()) {
      setError('Please enter your email address');
      return;
    }

    setLoading(true);
    setError('');

    const actionCodeSettings = {
      url: window.location.href,
      handleCodeInApp: true,
    };

    try {
      await sendSignInLinkToEmail(auth, email, actionCodeSettings);
      
      // Save email to localStorage for same-device sign-in
      window.localStorage.setItem('emailForSignIn', email);
      
      setEmailSent(true);
    } catch (err: any) {
      console.error('Error sending email link:', err);
      setError(err.message || 'Failed to send email link. Please try again.');
    } finally {
      setLoading(false);
    }
  };

  const handleGoogleSignIn = async () => {
    setLoading(true);
    setError('');

    const provider = new GoogleAuthProvider();

    try {
      const result = await signInWithPopup(auth, provider);
      
      if (onLoginSuccess) {
        onLoginSuccess(result.user);
      }
    } catch (err: any) {
      console.error('Error with Google sign-in:', err);
      setError(err.message || 'Failed to sign in with Google');
    } finally {
      setLoading(false);
    }
  };

  const handleGithubSignIn = async () => {
    setLoading(true);
    setError('');

    const provider = new GithubAuthProvider();

    try {
      const result = await signInWithPopup(auth, provider);
      
      if (onLoginSuccess) {
        onLoginSuccess(result.user);
      }
    } catch (err: any) {
      console.error('Error with GitHub sign-in:', err);
      setError(err.message || 'Failed to sign in with GitHub');
    } finally {
      setLoading(false);
    }
  };

  const handleEmailConfirmationSuccess = () => {
    setShowEmailConfirmation(false);
    
    if (onLoginSuccess && auth.currentUser) {
      onLoginSuccess(auth.currentUser);
    }
  };

  if (processingEmailLink) {
    return (
      <PageContainer>
        <Box textAlign="center">
          <CircularProgress size={60} sx={{ color: 'white', mb: 2 }} />
          <Typography variant="h6" color="white">
            Signing you in...
          </Typography>
        </Box>
      </PageContainer>
    );
  }

  if (showEmailConfirmation) {
    return (
      <PageContainer>
        <Fade in={true} timeout={500}>
          <StyledCard>
            <CardContent sx={{ p: 4 }}>
              <EmailConfirmation onSuccess={handleEmailConfirmationSuccess} />
            </CardContent>
          </StyledCard>
        </Fade>
      </PageContainer>
    );
  }

  if (emailSent) {
    return (
      <PageContainer>
        <Fade in={true} timeout={500}>
          <StyledCard>
            <CardContent sx={{ p: 4 }}>
              <EmailSentContainer>
                <SuccessIconWrapper>
                  <CheckCircleIcon sx={{ fontSize: 50, color: 'white' }} />
                </SuccessIconWrapper>
                
                <Typography variant="h5" gutterBottom fontWeight={600}>
                  Check Your Email!
                </Typography>
                
                <Typography variant="body1" color="text.secondary" paragraph>
                  We've sent a sign-in link to
                </Typography>
                
                <Typography 
                  variant="body1" 
                  color="primary" 
                  fontWeight={600}
                  paragraph
                >
                  {email}
                </Typography>
                
                <Typography variant="body2" color="text.secondary" paragraph>
                  Click the link in your email to complete sign-in. 
                  The link will expire in 60 minutes.
                </Typography>
                
                <ProviderButton
                  provider="email"
                  onClick={() => {
                    setEmailSent(false);
                    setEmail('');
                  }}
                  sx={{ mt: 2 }}
                >
                  Use a different email
                </ProviderButton>
              </EmailSentContainer>
            </CardContent>
          </StyledCard>
        </Fade>
      </PageContainer>
    );
  }

  return (
    <PageContainer>
      <Fade in={true} timeout={500}>
        <StyledCard>
          <CardContent sx={{ p: 4 }}>
            <Header>
              <Typography variant="h4" gutterBottom fontWeight={700}>
                Welcome Back
              </Typography>
              <Typography variant="body1" color="text.secondary">
                Sign in to continue
              </Typography>
            </Header>

            {error && (
              <Alert 
                severity="error" 
                onClose={() => setError('')}
                sx={{ mb: 3 }}
              >
                {error}
              </Alert>
            )}

            <ProviderSection>
              <ProviderButton
                provider="google"
                icon={<GoogleIcon />}
                onClick={handleGoogleSignIn}
                disabled={loading}
              >
                Continue with Google
              </ProviderButton>

              <ProviderButton
                provider="github"
                icon={<GitHubIcon />}
                onClick={handleGithubSignIn}
                disabled={loading}
              >
                Continue with GitHub
              </ProviderButton>
            </ProviderSection>

            <DividerWithText>
              <Divider sx={{ flex: 1 }} />
              <Typography variant="body2" color="text.secondary">
                OR
              </Typography>
              <Divider sx={{ flex: 1 }} />
            </DividerWithText>

            <form onSubmit={handleEmailLinkAuth}>
              <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2 }}>
                <TextField
                  fullWidth
                  label="Email Address"
                  type="email"
                  value={email}
                  onChange={(e) => setEmail(e.target.value)}
                  disabled={loading}
                  variant="outlined"
                  placeholder="Enter your email"
                />

                <ProviderButton
                  provider="email"
                  type="submit"
                  icon={loading ? <CircularProgress size={20} color="inherit" /> : <EmailIcon />}
                  disabled={loading}
                >
                  {loading ? 'Sending...' : 'Continue with Email'}
                </ProviderButton>
              </Box>
            </form>

            <Typography 
              variant="caption" 
              color="text.secondary" 
              sx={{ display: 'block', textAlign: 'center', mt: 3 }}
            >
              We'll email you a magic link for password-free sign in
            </Typography>
          </CardContent>
        </StyledCard>
      </Fade>
    </PageContainer>
  );
};

export default Login;
