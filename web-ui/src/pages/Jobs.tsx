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
  FormControlLabel,
  MenuItem,
  Paper,
  Stack,
  Switch,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableFooter,
  TableHead,
  TablePagination,
  TableRow,
  TextField,
  Tooltip,
  Typography,
  Select,
  InputLabel,
  FormControl,
} from '@mui/material';
import AddIcon from '@mui/icons-material/Add';
import DownloadIcon from '@mui/icons-material/Download';
import { useAuth } from '../hooks/useAuth';
import { listJobs, createJob, getJobLimits } from '../api/jobs';
import { listJobSpecs } from '../api/jobSpecs';
import type { ThumbnailsGenerationJobDTO, JobSpecDTO } from '../types/api.types';

const fmt = (ts: string | null) =>
  ts ? new Date(ts).toLocaleString() : '—';

const STATUS_COLOR: Record<
  ThumbnailsGenerationJobDTO['status'],
  'default' | 'info' | 'warning' | 'success' | 'error'
> = {
  SUBMITTED: 'info',
  IN_PROGRESS: 'warning',
  SUCCESS: 'success',
  FAILURE: 'error',
};

export function Jobs() {
  const { user, accountId } = useAuth();
  const [jobs, setJobs] = useState<ThumbnailsGenerationJobDTO[]>([]);
  const [specs, setSpecs] = useState<JobSpecDTO[]>([]);
  const [loading, setLoading] = useState(true);
  const [page, setPage] = useState(0);
  const [pageSize, setPageSize] = useState(20);
  const [totalElements, setTotalElements] = useState(0);
  const [dialogOpen, setDialogOpen] = useState(false);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState('');

  const [videoURL, setVideoURL] = useState('');
  const [jobSpecId, setJobSpecId] = useState('');
  const [streamIndex, setStreamIndex] = useState('');
  const [preview, setPreview] = useState(false);
  const [maxFileSizeBytes, setMaxFileSizeBytes] = useState<number | null>(null);

  useEffect(() => {
    getJobLimits().then((l) => setMaxFileSizeBytes(l.maxFileSizeBytes)).catch(() => {});
  }, []);

  useEffect(() => {
    if (!user || !accountId) return;
    setLoading(true);
    Promise.all([listJobs(user, accountId, page, pageSize), listJobSpecs(user, accountId)])
      .then(([j, s]) => { setJobs(j.content); setTotalElements(j.totalElements); setSpecs(s); })
      .catch(() => setError('Failed to load data'))
      .finally(() => setLoading(false));
  }, [user, accountId, page, pageSize]);

  const openDialog = () => {
    setVideoURL('');
    setJobSpecId(specs[0]?.id ?? '');
    setStreamIndex('');
    setPreview(false);
    setError('');
    setDialogOpen(true);
  };

  const handleCreate = async () => {
    if (!user || !accountId) return;
    setSaving(true);
    setError('');
    try {
      const parsedStreamIndex = streamIndex.trim() !== '' ? parseInt(streamIndex, 10) : null;
      await createJob(user, accountId, { videoURL, jobSpecId, streamIndex: parsedStreamIndex, preview });
      setPage(0);
      const refreshed = await listJobs(user, accountId, 0, pageSize);
      setJobs(refreshed.content);
      setTotalElements(refreshed.totalElements);
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
                <TableCell>Created</TableCell>
                <TableCell>Started</TableCell>
                <TableCell>Finished</TableCell>
                <TableCell>Download</TableCell>
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
                  <TableCell>
                    <Typography variant="body2" color="text.secondary" noWrap>{fmt(job.createdAt)}</Typography>
                  </TableCell>
                  <TableCell>
                    <Typography variant="body2" color="text.secondary" noWrap>{fmt(job.startedAt)}</Typography>
                  </TableCell>
                  <TableCell>
                    <Typography variant="body2" color="text.secondary" noWrap>{fmt(job.finishedAt)}</Typography>
                  </TableCell>
                  <TableCell>
                    <Tooltip title={
                      job.downloadUrl
                        ? 'Download sprites as ZIP'
                        : job.status === 'SUCCESS'
                          ? 'Download link expired (available for 2 hours after completion)'
                          : job.status === 'FAILURE'
                            ? 'Job failed'
                            : 'Job not finished yet'
                    }>
                      <span>
                        <Button
                          size="small"
                          variant="outlined"
                          startIcon={<DownloadIcon />}
                          disabled={!job.downloadUrl}
                          href={job.downloadUrl ?? undefined}
                          target="_blank"
                          rel="noopener noreferrer"
                          component={job.downloadUrl ? 'a' : 'button'}
                        >
                          Download
                        </Button>
                      </span>
                    </Tooltip>
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
            <TableFooter>
              <TableRow>
                <TablePagination
                  count={totalElements}
                  page={page}
                  rowsPerPage={pageSize}
                  rowsPerPageOptions={[10, 20, 50]}
                  onPageChange={(_, newPage) => setPage(newPage)}
                  onRowsPerPageChange={(e) => { setPageSize(parseInt(e.target.value, 10)); setPage(0); }}
                />
              </TableRow>
            </TableFooter>
          </Table>
        </TableContainer>
      )}

      <Dialog open={dialogOpen} onClose={() => setDialogOpen(false)} maxWidth="sm" fullWidth>
        <DialogTitle>New Job</DialogTitle>
        <DialogContent>
          <Stack spacing={2} mt={1}>
            {maxFileSizeBytes !== null && (
              <Alert severity="info">
                Video file must not exceed {(maxFileSizeBytes / 1_073_741_824).toFixed(0)} GB.
              </Alert>
            )}
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
            <TextField
              label="Stream index"
              value={streamIndex}
              onChange={(e) => setStreamIndex(e.target.value)}
              type="number"
              fullWidth
              helperText="Optional — leave blank to auto-detect the first video stream"
              inputProps={{ min: 0 }}
            />
            <FormControlLabel
              control={<Switch checked={preview} onChange={(e) => setPreview(e.target.checked)} />}
              label="Upload unzipped preview files"
            />
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
