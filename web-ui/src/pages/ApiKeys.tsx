import { useEffect, useState } from 'react';
import {
  Alert,
  Box,
  Button,
  Chip,
  CircularProgress,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  IconButton,
  InputAdornment,
  Paper,
  Stack,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  TextField,
  Tooltip,
  Typography,
} from '@mui/material';
import AddIcon from '@mui/icons-material/Add';
import DeleteIcon from '@mui/icons-material/Delete';
import ContentCopyIcon from '@mui/icons-material/ContentCopy';
import { useAuth } from '../hooks/useAuth';
import { listApiKeys, createApiKey, disableApiKey, enableApiKey, deleteApiKey } from '../api/apiKeys';
import type { ApiKeyDTO } from '../types/api.types';

export function ApiKeys() {
  const { user, userId, accountId } = useAuth();
  const [keys, setKeys] = useState<ApiKeyDTO[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  const [createOpen, setCreateOpen] = useState(false);
  const [description, setDescription] = useState('');
  const [saving, setSaving] = useState(false);
  const [createError, setCreateError] = useState('');

  const [createdKey, setCreatedKey] = useState<{ id: string; key: string } | null>(null);
  const [copied, setCopied] = useState(false);

  const [actionLoading, setActionLoading] = useState<string | null>(null);

  useEffect(() => {
    if (!user || !userId || !accountId) return;
    listApiKeys(user, userId, accountId)
      .then(setKeys)
      .catch(() => setError('Failed to load API keys'))
      .finally(() => setLoading(false));
  }, [user, userId, accountId]);

  const openCreate = () => {
    setDescription('');
    setCreateError('');
    setCreateOpen(true);
  };

  const handleCreate = async () => {
    if (!user || !userId || !accountId) return;
    setSaving(true);
    setCreateError('');
    try {
      const created = await createApiKey(user, userId, accountId, description);
      setKeys((prev) => [...prev, { ...created, key: null }]);
      setCreateOpen(false);
      setCreatedKey({ id: created.id, key: created.key! });
    } catch (e) {
      setCreateError(e instanceof Error ? e.message : 'Failed to create API key');
    } finally {
      setSaving(false);
    }
  };

  const handleToggle = async (key: ApiKeyDTO) => {
    if (!user || !userId || !accountId) return;
    setActionLoading(key.id);
    try {
      const updated = key.disabled
        ? await enableApiKey(user, userId, accountId, key.id)
        : await disableApiKey(user, userId, accountId, key.id);
      setKeys((prev) => prev.map((k) => (k.id === updated.id ? updated : k)));
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Action failed');
    } finally {
      setActionLoading(null);
    }
  };

  const handleDelete = async (key: ApiKeyDTO) => {
    if (!user || !userId || !accountId) return;
    setActionLoading(key.id);
    try {
      await deleteApiKey(user, userId, accountId, key.id);
      setKeys((prev) => prev.filter((k) => k.id !== key.id));
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Failed to delete API key');
    } finally {
      setActionLoading(null);
    }
  };

  const handleCopy = () => {
    if (createdKey) {
      navigator.clipboard.writeText(createdKey.key);
      setCopied(true);
      setTimeout(() => setCopied(false), 2000);
    }
  };

  return (
    <Box>
      <Stack direction="row" justifyContent="space-between" alignItems="center" mb={3}>
        <Typography variant="h5" fontWeight="bold">API Keys</Typography>
        <Button
          variant="contained"
          color="secondary"
          startIcon={<AddIcon />}
          onClick={openCreate}
          disabled={keys.length >= 2}
        >
          New API Key
        </Button>
      </Stack>

      {error && <Alert severity="error" sx={{ mb: 2 }} onClose={() => setError('')}>{error}</Alert>}

      {loading ? (
        <Box display="flex" justifyContent="center" mt={6}><CircularProgress /></Box>
      ) : keys.length === 0 ? (
        <Typography color="text.secondary">No API keys yet.</Typography>
      ) : (
        <TableContainer component={Paper} variant="outlined">
          <Table>
            <TableHead>
              <TableRow>
                <TableCell>Description</TableCell>
                <TableCell>Key ID</TableCell>
                <TableCell>Status</TableCell>
                <TableCell>Last Used</TableCell>
                <TableCell align="right">Actions</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {keys.map((k) => (
                <TableRow key={k.id} hover>
                  <TableCell>
                    <Typography variant="body2" fontWeight="medium">{k.description}</Typography>
                  </TableCell>
                  <TableCell>
                    <Typography variant="body2" fontFamily="monospace" color="text.secondary">{k.id}</Typography>
                  </TableCell>
                  <TableCell>
                    <Chip
                      size="small"
                      label={k.disabled ? 'Disabled' : 'Active'}
                      color={k.disabled ? 'default' : 'success'}
                    />
                  </TableCell>
                  <TableCell>
                    <Typography variant="body2" color="text.secondary">
                      {k.lastAccessTime
                        ? new Date(k.lastAccessTime).toLocaleString()
                        : 'Never'}
                    </Typography>
                  </TableCell>
                  <TableCell align="right">
                    <Stack direction="row" justifyContent="flex-end" gap={1}>
                      <Button
                        size="small"
                        variant="outlined"
                        color={k.disabled ? 'success' : 'warning'}
                        disabled={actionLoading === k.id}
                        onClick={() => handleToggle(k)}
                      >
                        {actionLoading === k.id ? (
                          <CircularProgress size={16} />
                        ) : k.disabled ? 'Enable' : 'Disable'}
                      </Button>
                      <Tooltip title={k.disabled ? 'Delete' : 'Disable the key first to delete it'}>
                        <span>
                          <IconButton
                            size="small"
                            color="error"
                            disabled={!k.disabled || actionLoading === k.id}
                            onClick={() => handleDelete(k)}
                          >
                            <DeleteIcon fontSize="small" />
                          </IconButton>
                        </span>
                      </Tooltip>
                    </Stack>
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </TableContainer>
      )}

      {/* Create dialog */}
      <Dialog open={createOpen} onClose={() => setCreateOpen(false)} maxWidth="sm" fullWidth>
        <DialogTitle>New API Key</DialogTitle>
        <DialogContent>
          <Stack spacing={2} mt={1}>
            <TextField
              label="Description"
              value={description}
              onChange={(e) => setDescription(e.target.value)}
              fullWidth
              required
              placeholder="e.g. CI pipeline"
            />
            {createError && <Typography color="error">{createError}</Typography>}
          </Stack>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setCreateOpen(false)}>Cancel</Button>
          <Button
            variant="contained"
            onClick={handleCreate}
            disabled={saving || !description.trim()}
          >
            {saving ? <CircularProgress size={20} /> : 'Create'}
          </Button>
        </DialogActions>
      </Dialog>

      {/* Reveal key dialog — shown only once after creation */}
      <Dialog open={!!createdKey} onClose={() => setCreatedKey(null)} maxWidth="sm" fullWidth>
        <DialogTitle>API Key Created</DialogTitle>
        <DialogContent>
          <Stack spacing={2} mt={1}>
            <Alert severity="warning">
              Copy this key now — it will not be shown again.
            </Alert>
            <TextField
              label="Key ID"
              value={createdKey?.id ?? ''}
              fullWidth
              InputProps={{
                readOnly: true,
                endAdornment: (
                  <InputAdornment position="end">
                    <Tooltip title="Copy">
                      <IconButton onClick={() => navigator.clipboard.writeText(createdKey?.id ?? '')} edge="end">
                        <ContentCopyIcon fontSize="small" />
                      </IconButton>
                    </Tooltip>
                  </InputAdornment>
                ),
              }}
            />
            <TextField
              label="API Key"
              value={createdKey?.key ?? ''}
              fullWidth
              InputProps={{
                readOnly: true,
                endAdornment: (
                  <InputAdornment position="end">
                    <Tooltip title={copied ? 'Copied!' : 'Copy'}>
                      <IconButton onClick={handleCopy} edge="end">
                        <ContentCopyIcon fontSize="small" />
                      </IconButton>
                    </Tooltip>
                  </InputAdornment>
                ),
              }}
            />
          </Stack>
        </DialogContent>
        <DialogActions>
          <Button variant="contained" onClick={() => setCreatedKey(null)}>Done</Button>
        </DialogActions>
      </Dialog>
    </Box>
  );
}
