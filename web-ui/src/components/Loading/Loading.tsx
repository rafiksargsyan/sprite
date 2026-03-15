import React from 'react';
import { Box, CircularProgress, Typography } from '@mui/material';
import { styled } from '@mui/material/styles';

const LoadingContainer = styled(Box)(({ theme }) => ({
  display: 'flex',
  flexDirection: 'column',
  justifyContent: 'center',
  alignItems: 'center',
  minHeight: '100vh',
  gap: theme.spacing(2),
}));

const Loading: React.FC = () => {
  return (
    <LoadingContainer>
      <CircularProgress size={60} />
      <Typography variant="h6" color="text.secondary">
        Loading...
      </Typography>
    </LoadingContainer>
  );
};

export default Loading;