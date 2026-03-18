import { useEffect, useState } from 'react';
import {
  Box,
  Button,
  Chip,
  FormControl,
  InputLabel,
  CircularProgress,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  Divider,
  FormControlLabel,
  IconButton,
  MenuItem,
  Paper,
  Select,
  Stack,
  Switch,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  TextField,
  Typography,
} from '@mui/material';
import AddIcon from '@mui/icons-material/Add';
import DeleteIcon from '@mui/icons-material/Delete';
import { useAuth } from '../hooks/useAuth';
import { listJobSpecs, createJobSpec } from '../api/jobSpecs';
import type { JobSpecDTO, ThumbnailConfigRequest } from '../types/api.types';

type WebpPreset = 'default' | 'picture' | 'photo' | 'drawing' | 'icon' | 'text';

interface ConfigDraft {
  format: 'jpg' | 'webp' | 'avif' | 'blurhash';
  resolution: number;
  spriteRows: number;
  spriteCols: number;
  quality: number;
  interval: number;
  method: number;
  lossless: boolean;
  preset: WebpPreset;
  speed: number;
  componentsX: number;
  componentsY: number;
  folderName: string;
}

const defaultConfig = (): ConfigDraft => ({
  format: 'jpg',
  resolution: 120,
  spriteRows: 5,
  spriteCols: 5,
  quality: 85,
  interval: 10,
  method: 4,
  lossless: false,
  preset: 'default',
  speed: 6,
  componentsX: 4,
  componentsY: 3,
  folderName: '',
});

function configDraftToRequest(c: ConfigDraft): ThumbnailConfigRequest {
  const spriteSize = { rows: c.spriteRows, cols: c.spriteCols };
  if (c.format === 'jpg') {
    return { format: 'jpg', resolution: c.resolution, spriteSize, quality: c.quality, interval: c.interval, folderName: c.folderName };
  }
  if (c.format === 'avif') {
    return { format: 'avif', resolution: c.resolution, spriteSize, quality: c.quality, interval: c.interval, speed: c.speed, folderName: c.folderName };
  }
  if (c.format === 'blurhash') {
    return { format: 'blurhash', resolution: c.resolution, interval: c.interval, componentsX: c.componentsX, componentsY: c.componentsY, folderName: c.folderName };
  }
  return { format: 'webp', resolution: c.resolution, spriteSize, quality: c.quality, interval: c.interval, method: c.method, lossless: c.lossless, preset: c.preset, folderName: c.folderName };
}

export function JobSpecs() {
  const { user, accountId } = useAuth();
  const [specs, setSpecs] = useState<JobSpecDTO[]>([]);
  const [loading, setLoading] = useState(true);
  const [dialogOpen, setDialogOpen] = useState(false);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState('');

  const [name, setName] = useState('');
  const [description, setDescription] = useState('');
  const [configs, setConfigs] = useState<ConfigDraft[]>([defaultConfig()]);

  useEffect(() => {
    if (!user || !accountId) return;
    listJobSpecs(user, accountId)
      .then(setSpecs)
      .catch(() => setError('Failed to load job specs'))
      .finally(() => setLoading(false));
  }, [user, accountId]);

  const openDialog = () => {
    setName('');
    setDescription('');
    setConfigs([defaultConfig()]);
    setError('');
    setDialogOpen(true);
  };

  const addConfig = () => setConfigs((prev) => [...prev, defaultConfig()]);

  const removeConfig = (i: number) =>
    setConfigs((prev) => prev.filter((_, idx) => idx !== i));

  const updateConfig = (i: number, patch: Partial<ConfigDraft>) =>
    setConfigs((prev) => prev.map((c, idx) => (idx === i ? { ...c, ...patch } : c)));

  const handleSave = async () => {
    if (!user || !accountId) return;
    setSaving(true);
    setError('');
    try {
      const created = await createJobSpec(user, accountId, {
        name,
        description: description || undefined,
        configs: configs.map(configDraftToRequest),
      });
      setSpecs((prev) => [created, ...prev]);
      setDialogOpen(false);
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Failed to create spec');
    } finally {
      setSaving(false);
    }
  };

  return (
    <Box>
      <Stack direction="row" justifyContent="space-between" alignItems="center" mb={3}>
        <Typography variant="h5" fontWeight="bold">Job Specs</Typography>
        <Button variant="contained" startIcon={<AddIcon />} onClick={openDialog}>
          New Spec
        </Button>
      </Stack>

      {loading ? (
        <Box display="flex" justifyContent="center" mt={6}><CircularProgress /></Box>
      ) : specs.length === 0 ? (
        <Typography color="text.secondary">No job specs yet. Create one to get started.</Typography>
      ) : (
        <TableContainer component={Paper} variant="outlined">
          <Table>
            <TableHead>
              <TableRow>
                <TableCell>Name</TableCell>
                <TableCell>Description</TableCell>
                <TableCell>Configs</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {specs.map((s) => (
                <TableRow key={s.id} hover>
                  <TableCell><Typography fontWeight="medium">{s.name}</Typography></TableCell>
                  <TableCell><Typography color="text.secondary">{s.description ?? '—'}</Typography></TableCell>
                  <TableCell>
                    <Stack direction="row" gap={0.5} flexWrap="wrap">
                      {s.configs.map((c, i) => (
                        <Chip key={i} size="small" label={`${c.format} ${c.resolution}p`} />
                      ))}
                    </Stack>
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </TableContainer>
      )}

      <Dialog open={dialogOpen} onClose={() => setDialogOpen(false)} maxWidth="md" fullWidth>
        <DialogTitle>New Job Spec</DialogTitle>
        <DialogContent>
          <Stack spacing={2} mt={1}>
            <TextField label="Name" value={name} onChange={(e) => setName(e.target.value)} required fullWidth />
            <TextField label="Description" value={description} onChange={(e) => setDescription(e.target.value)} fullWidth />

            <Divider />
            <Stack direction="row" justifyContent="space-between" alignItems="center">
              <Typography variant="subtitle1" fontWeight="medium">Configs</Typography>
              <Button size="small" startIcon={<AddIcon />} onClick={addConfig} disabled={configs.length >= 10}>
                Add Config
              </Button>
            </Stack>

            {configs.map((cfg, i) => (
              <Paper key={i} variant="outlined" sx={{ p: 2 }}>
                <Stack spacing={2}>
                  <Stack direction="row" justifyContent="space-between" alignItems="center">
                    <Typography variant="body2" fontWeight="medium">Config {i + 1}</Typography>
                    <IconButton size="small" onClick={() => removeConfig(i)} disabled={configs.length === 1}>
                      <DeleteIcon fontSize="small" />
                    </IconButton>
                  </Stack>
                  <TextField
                    size="small"
                    label="Folder name"
                    value={cfg.folderName}
                    onChange={(e) => updateConfig(i, { folderName: e.target.value })}
                    fullWidth
                    helperText="1–63 chars: letters, digits, hyphens, underscores, periods"
                  />
                  <Stack direction="row" spacing={2} flexWrap="wrap">
                    <Select
                      size="small"
                      value={cfg.format}
                      onChange={(e) => updateConfig(i, { format: e.target.value as ConfigDraft['format'], quality: e.target.value === 'jpg' ? 85 : 60 })}
                      sx={{ minWidth: 100 }}
                    >
                      <MenuItem value="jpg">JPG</MenuItem>
                      <MenuItem value="webp">WebP</MenuItem>
                      <MenuItem value="avif">AVIF</MenuItem>
                      <MenuItem value="blurhash">Blurhash</MenuItem>
                    </Select>
                    <TextField size="small" label="Resolution (px height)" type="number" value={cfg.resolution}
                      onChange={(e) => updateConfig(i, { resolution: +e.target.value })} sx={{ width: 160 }} />
                    <TextField size="small" label="Interval (s)" type="number" value={cfg.interval}
                      onChange={(e) => updateConfig(i, { interval: +e.target.value })} sx={{ width: 110 }} />
                    {cfg.format !== 'blurhash' && (
                      <>
                        <TextField size="small" label="Quality" type="number" value={cfg.quality}
                          onChange={(e) => updateConfig(i, { quality: +e.target.value })} sx={{ width: 90 }} />
                        <TextField size="small" label="Sprite rows" type="number" value={cfg.spriteRows}
                          onChange={(e) => updateConfig(i, { spriteRows: +e.target.value })} sx={{ width: 100 }} />
                        <TextField size="small" label="Sprite cols" type="number" value={cfg.spriteCols}
                          onChange={(e) => updateConfig(i, { spriteCols: +e.target.value })} sx={{ width: 100 }} />
                      </>
                    )}
                  </Stack>
                  {cfg.format === 'blurhash' && (
                    <Stack direction="row" spacing={2} flexWrap="wrap" alignItems="center">
                      <TextField size="small" label="Components X (1–9)" type="number" value={cfg.componentsX}
                        onChange={(e) => updateConfig(i, { componentsX: +e.target.value })} sx={{ width: 150 }} />
                      <TextField size="small" label="Components Y (1–9)" type="number" value={cfg.componentsY}
                        onChange={(e) => updateConfig(i, { componentsY: +e.target.value })} sx={{ width: 150 }} />
                    </Stack>
                  )}
                  {cfg.format === 'avif' && (
                    <Stack direction="row" spacing={2} flexWrap="wrap" alignItems="center">
                      <TextField size="small" label="Speed (0–8)" type="number" value={cfg.speed}
                        onChange={(e) => updateConfig(i, { speed: +e.target.value })} sx={{ width: 110 }} />
                    </Stack>
                  )}
                  {cfg.format === 'webp' && (
                    <Stack direction="row" spacing={2} flexWrap="wrap" alignItems="center">
                      <FormControl size="small" sx={{ minWidth: 110 }}>
                        <InputLabel>Preset</InputLabel>
                        <Select
                          label="Preset"
                          value={cfg.preset}
                          onChange={(e) => updateConfig(i, { preset: e.target.value as WebpPreset })}
                        >
                          {(['default', 'picture', 'photo', 'drawing', 'icon', 'text'] as WebpPreset[]).map((p) => (
                            <MenuItem key={p} value={p}>{p.charAt(0).toUpperCase() + p.slice(1)}</MenuItem>
                          ))}
                        </Select>
                      </FormControl>
                      <TextField size="small" label="Method (0–6)" type="number" value={cfg.method}
                        onChange={(e) => updateConfig(i, { method: +e.target.value })} sx={{ width: 110 }} />
                      <FormControlLabel
                        control={<Switch checked={cfg.lossless} onChange={(e) => updateConfig(i, { lossless: e.target.checked })} />}
                        label="Lossless"
                      />
                    </Stack>
                  )}
                </Stack>
              </Paper>
            ))}

            {error && <Typography color="error">{error}</Typography>}
          </Stack>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setDialogOpen(false)}>Cancel</Button>
          <Button variant="contained" onClick={handleSave} disabled={saving || !name.trim() || configs.some((c) => !c.folderName.trim())}>
            {saving ? <CircularProgress size={20} /> : 'Create'}
          </Button>
        </DialogActions>
      </Dialog>
    </Box>
  );
}
