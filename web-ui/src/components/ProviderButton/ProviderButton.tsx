import React from 'react';
import { Button } from '@mui/material';
import type { ButtonProps } from '@mui/material';
import { styled } from '@mui/material/styles';

interface ProviderButtonProps extends ButtonProps {
  provider: 'google' | 'github' | 'email';
  icon?: React.ReactNode;
}

const StyledButton = styled(Button)<{ provider: string }>(({ theme, provider }) => {
  const colors = {
    google: {
      background: '#fff',
      color: '#757575',
      border: '1px solid #dadce0',
      '&:hover': {
        background: '#f8f9fa',
      }
    },
    github: {
      background: '#24292e',
      color: '#fff',
      '&:hover': {
        background: '#1a1e22',
      }
    },
    email: {
      background: theme.palette.primary.main,
      color: '#fff',
      '&:hover': {
        background: theme.palette.primary.dark,
      }
    }
  };

  return {
    padding: theme.spacing(1.5, 3),
    borderRadius: theme.spacing(1),
    textTransform: 'none',
    fontSize: '16px',
    fontWeight: 500,
    width: '100%',
    justifyContent: 'flex-start',
    gap: theme.spacing(2),
    transition: 'all 0.3s ease',
    ...colors[provider as keyof typeof colors],
  };
});

const ProviderButton: React.FC<ProviderButtonProps> = ({ 
  provider, 
  icon, 
  children, 
  ...props 
}) => {
  return (
    <StyledButton
      provider={provider}
      startIcon={icon}
      {...props}
    >
      {children}
    </StyledButton>
  );
};

export default ProviderButton;