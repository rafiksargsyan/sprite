import { useEffect, useState } from 'react';
import {
  Box,
  Button,
  Chip,
  CircularProgress,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  MenuItem,
  Paper,
  Stack,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  TextField,
  Typography,
  Select,
  InputLabel,
  FormControl,
} from '@mui/material';
import AddIcon from '@mui/icons-material/Add';
import { useAuth } from '../hooks/useAuth';
import { listJobs, createJob } from '../api/jobs';
import { listJobSpecs } from '../api/jobSpecs';
import type { ThumbnailsGenerationJobDTO, JobSpecDTO } from '../types/api.types';

const STATUS_COLOR: Record<
  ThumbnailsGenerationJobDTO['status'],
  'default' | 'info' | 'warning' | 'success' | 'error'
> = {
  SUBMITTED: 'info',
  QUEUED: 'warning',
  IN_PROGRESS: 'warning',
  SUCCESS: 'success',
  FAILURE: 'error',
};

export function Jobs() {
  const { user, accountId } = useAuth();
  const [jobs, setJobs] = useState<ThumbnailsGenerationJobDTO[]>([]);
  const [specs, setSpecs] = useState<JobSpecDTO[]>([]);
  const [loading, setLoading] = useState(true);
  const [dialogOpen, setDialogOpen] = useState(false);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState('');

  const [videoURL, setVideoURL] = useState('');
  const [jobSpecId, setJobSpecId] = useState('');

  useEffect(() => {
    if (!user || !accountId) return;
    Promise.all([listJobs(user, accountId), listJobSpecs(user, accountId)])
      .then(([j, s]) => { setJobs(j); setSpecs(s); })
      .catch(() => setError('Failed to load data'))
      .finally(() => setLoading(false));
  }, [user, accountId]);

  const openDialog = () => {
    setVideoURL('');
    setJobSpecId(specs[0]?.id ?? '');
    setError('');
    setDialogOpen(true);
  };

  const handleCreate = async () => {
    if (!user || !accountId) return;
    setSaving(true);
    setError('');
    try {
      const created = await createJob(user, accountId, { videoURL, jobSpecId });
      setJobs((prev) => [created, ...prev]);
      setDialogOpen(false);
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Failed to create job');
    } finally {
      setSaving(false);
    }
  };

  return (
    <Box>
      <Stack direction="row" justifyContent="space-between" alignItems="center" mb={3}>
        <Typography variant="h5" fontWeight="bold">Jobs</Typography>
        <Button variant="contained" startIcon={<AddIcon />} onClick={openDialog} disabled={specs.length === 0}>
          New Job
        </Button>
      </Stack>

      {specs.length === 0 && !loading && (
        <Typography color="text.secondary" mb={2}>
          Create a job spec first before submitting jobs.
        </Typography>
      )}

      {loading ? (
        <Box display="flex" justifyContent="center" mt={6}><CircularProgress /></Box>
      ) : jobs.length === 0 ? (
        <Typography color="text.secondary">No jobs yet.</Typography>
      ) : (
        <TableContainer component={Paper} variant="outlined">
          <Table>
            <TableHead>
              <TableRow>
                <TableCell>ID</TableCell>
                <TableCell>Video URL</TableCell>
                <TableCell>Configs</TableCell>
                <TableCell>Status</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {jobs.map((job) => (
                <TableRow key={job.id} hover>
                  <TableCell>
                    <Typography variant="body2" fontFamily="monospace">
                      {job.id.slice(0, 12)}…
                    </Typography>
                  </TableCell>
                  <TableCell>
                    <Typography
                      variant="body2"
                      sx={{ maxWidth: 300, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}
                    >
                      {job.videoUrl}
                    </Typography>
                  </TableCell>
                  <TableCell>
                    <Stack direction="row" gap={0.5} flexWrap="wrap">
                      {job.jobSpec.configs.map((c, i) => (
                        <Chip key={i} size="small" label={`${c.format} ${c.resolution}p`} />
                      ))}
                    </Stack>
                  </TableCell>
                  <TableCell>
                    <Chip
                      size="small"
                      label={job.status}
                      color={STATUS_COLOR[job.status]}
                    />
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </TableContainer>
      )}

      <Dialog open={dialogOpen} onClose={() => setDialogOpen(false)} maxWidth="sm" fullWidth>
        <DialogTitle>New Job</DialogTitle>
        <DialogContent>
          <Stack spacing={2} mt={1}>
            <TextField
              label="Video URL"
              value={videoURL}
              onChange={(e) => setVideoURL(e.target.value)}
              fullWidth
              required
              placeholder="https://example.com/video.mp4"
            />
            <FormControl fullWidth required>
              <InputLabel>Job Spec</InputLabel>
              <Select
                label="Job Spec"
                value={jobSpecId}
                onChange={(e) => setJobSpecId(e.target.value)}
              >
                {specs.map((s) => (
                  <MenuItem key={s.id} value={s.id}>{s.name}</MenuItem>
                ))}
              </Select>
            </FormControl>
            {error && <Typography color="error">{error}</Typography>}
          </Stack>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setDialogOpen(false)}>Cancel</Button>
          <Button
            variant="contained"
            onClick={handleCreate}
            disabled={saving || !videoURL.trim() || !jobSpecId}
          >
            {saving ? <CircularProgress size={20} /> : 'Submit'}
          </Button>
        </DialogActions>
      </Dialog>
    </Box>
  );
}
