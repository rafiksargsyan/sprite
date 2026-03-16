import { Box, Typography, Paper } from '@mui/material';
import { Grid } from '@mui/material';
import { useAuth } from '../hooks/useAuth';

const STATS = [
  { label: 'Total Jobs', value: '—' },
  { label: 'Processing', value: '—' },
  { label: 'Completed', value: '—' },
  { label: 'API Calls', value: '—' },
];

export function Dashboard() {
  const { user } = useAuth();
  const name = user?.displayName || user?.email?.split('@')[0] || 'there';

  return (
    <Box>
      <Typography variant="h5" fontWeight="bold" gutterBottom>
        Welcome back, {name}
      </Typography>
      <Typography variant="body1" color="text.secondary" sx={{ mb: 4 }}>
        Generate WebVTT thumbnails for your videos via UI or API.
      </Typography>

      <Grid container spacing={2}>
        {STATS.map(({ label, value }) => (
          <Grid key={label} size={{ xs: 12, sm: 6, md: 3 }}>
            <Paper sx={{ p: 3, textAlign: 'center' }} elevation={1}>
              <Typography variant="h4" fontWeight="bold">
                {value}
              </Typography>
              <Typography variant="body2" color="text.secondary" sx={{ mt: 0.5 }}>
                {label}
              </Typography>
            </Paper>
          </Grid>
        ))}
      </Grid>
    </Box>
  );
}
