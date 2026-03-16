import { useState } from 'react';
import {
  Dialog,
  DialogTitle,
  DialogContent,
  DialogContentText,
  DialogActions,
  TextField,
  Button,
  Alert,
} from '@mui/material';
import { useAuth } from '../../hooks/useAuth';

export function EmailConfirmation() {
  const { pendingEmailConfirmation, confirmEmailForLink } = useAuth();
  const [email, setEmail] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  const handleConfirm = async () => {
    if (!email) {
      setError('Please enter your email address');
      return;
    }
    setLoading(true);
    setError('');
    try {
      await confirmEmailForLink(email);
    } catch {
      setError('Invalid email or expired link. Please try again.');
      setLoading(false);
    }
  };

  return (
    <Dialog open={pendingEmailConfirmation} disableEscapeKeyDown>
      <DialogTitle>Confirm Your Email</DialogTitle>
      <DialogContent>
        <DialogContentText>
          You opened the sign-in link on a different device. Please enter the email address
          you used to request the link.
        </DialogContentText>
        {error && (
          <Alert severity="error" sx={{ mt: 2 }}>
            {error}
          </Alert>
        )}
        <TextField
          autoFocus
          margin="dense"
          label="Email address"
          type="email"
          fullWidth
          variant="outlined"
          value={email}
          onChange={(e) => setEmail(e.target.value)}
          onKeyDown={(e) => e.key === 'Enter' && handleConfirm()}
          sx={{ mt: 2 }}
        />
      </DialogContent>
      <DialogActions sx={{ px: 3, pb: 2 }}>
        <Button onClick={handleConfirm} variant="contained" disabled={loading}>
          {loading ? 'Signing in…' : 'Sign in'}
        </Button>
      </DialogActions>
    </Dialog>
  );
}
