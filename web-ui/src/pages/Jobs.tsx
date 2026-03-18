import { useEffect, useRef, useState } from 'react';
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
  Slider as MuiSlider,
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
import PlayCircleOutlineIcon from '@mui/icons-material/PlayCircleOutline';
import { useAuth } from '../hooks/useAuth';
import { listJobs, createJob, getJobLimits, getJobPreviewFiles, getJobPreviewVtt } from '../api/jobs';
import { listJobSpecs } from '../api/jobSpecs';
import type { ThumbnailsGenerationJobDTO, JobSpecDTO, PreviewFilesResponse } from '../types/api.types';

const fmt = (ts: string | null) =>
  ts ? new Date(ts).toLocaleString() : '—';

interface VttCue {
  start: number;
  end: number;
  file: string;
  x: number;
  y: number;
  w: number;
  h: number;
}

function parseTimestamp(ts: string): number {
  const [h, m, s] = ts.split(':');
  return parseInt(h) * 3600 + parseInt(m) * 60 + parseFloat(s);
}

function parseVtt(text: string): VttCue[] {
  const cues: VttCue[] = [];
  const lines = text.split('\n');
  for (let i = 0; i < lines.length; i++) {
    const match = lines[i].trim().match(/^(\d{2}:\d{2}:\d{2}\.\d{3})\s+-->\s+(\d{2}:\d{2}:\d{2}\.\d{3})$/);
    if (match) {
      const urlLine = lines[++i]?.trim() ?? '';
      const urlMatch = urlLine.match(/^(.+?)#xywh=(\d+),(\d+),(\d+),(\d+)$/);
      if (urlMatch) {
        cues.push({
          start: parseTimestamp(match[1]),
          end: parseTimestamp(match[2]),
          file: urlMatch[1],
          x: parseInt(urlMatch[2]),
          y: parseInt(urlMatch[3]),
          w: parseInt(urlMatch[4]),
          h: parseInt(urlMatch[5]),
        });
      }
    }
  }
  return cues;
}

function findCue(cues: VttCue[], time: number): VttCue | null {
  return cues.find(c => time >= c.start && time < c.end) ?? cues[cues.length - 1] ?? null;
}

interface PreviewDialogProps {
  job: ThumbnailsGenerationJobDTO;
  user: import('firebase/auth').User;
  accountId: string;
  onClose: () => void;
}

function PreviewDialog({ job, user, accountId, onClose }: PreviewDialogProps) {
  const configs = job.jobSpec.configs;
  const [selectedConfig, setSelectedConfig] = useState(configs[0]?.folderName ?? '');
  const [files, setFiles] = useState<PreviewFilesResponse | null>(null);
  const [cues, setCues] = useState<VttCue[]>([]);
  const [sliderValue, setSliderValue] = useState(0);
  const [displayWidth, setDisplayWidth] = useState(320);
  const [sheetSize, setSheetSize] = useState<{ w: number; h: number } | null>(null);
  const [loading, setLoading] = useState(false);
  const imgRef = useRef<HTMLImageElement>(null);

  useEffect(() => {
    if (!selectedConfig) return;
    setLoading(true);
    setFiles(null);
    setCues([]);
    setSheetSize(null);
    Promise.all([
      getJobPreviewFiles(user, accountId, job.id, selectedConfig),
      getJobPreviewVtt(user, accountId, job.id, selectedConfig),
    ])
      .then(([f, vtt]) => {
        setFiles(f);
        setCues(parseVtt(vtt));
        setSliderValue(0);
      })
      .finally(() => setLoading(false));
  }, [selectedConfig]);

  const maxTime = cues.length > 0 ? cues[cues.length - 1].end : 0;
  const cue = findCue(cues, sliderValue);
  const spriteUrl = cue && files ? files[cue.file] : null;
  const scale = cue ? displayWidth / cue.w : 1;
  const displayHeight = cue ? Math.round(cue.h * scale) : 0;

  return (
    <Dialog open onClose={onClose} maxWidth="md" fullWidth>
      <DialogTitle>Preview — {job.id.slice(0, 12)}…</DialogTitle>
      <DialogContent>
        <Stack spacing={3} mt={1}>
          <Stack direction="row" spacing={2} alignItems="center">
            <FormControl size="small" sx={{ minWidth: 180 }}>
              <InputLabel>Config</InputLabel>
              <Select
                label="Config"
                value={selectedConfig}
                onChange={(e) => setSelectedConfig(e.target.value)}
              >
                {configs.map((c) => (
                  <MenuItem key={c.folderName} value={c.folderName}>
                    {c.folderName}
                  </MenuItem>
                ))}
              </Select>
            </FormControl>
            <Box flex={1}>
              <Typography variant="caption" color="text.secondary">
                Thumbnail size: {displayWidth}px
              </Typography>
              <MuiSlider
                min={120}
                max={640}
                step={10}
                value={displayWidth}
                onChange={(_, v) => setDisplayWidth(v as number)}
                size="small"
              />
            </Box>
          </Stack>

          {loading && <Box display="flex" justifyContent="center"><CircularProgress /></Box>}

          {!loading && cue && spriteUrl && (
            <Stack spacing={1} alignItems="center">
              <Box
                sx={{
                  width: displayWidth,
                  height: displayHeight,
                  backgroundImage: `url(${spriteUrl})`,
                  backgroundPosition: `-${Math.round(cue.x * scale)}px -${Math.round(cue.y * scale)}px`,
                  backgroundSize: sheetSize
                    ? `${Math.round(sheetSize.w * scale)}px ${Math.round(sheetSize.h * scale)}px`
                    : 'auto',
                  backgroundRepeat: 'no-repeat',
                  border: '1px solid',
                  borderColor: 'divider',
                  flexShrink: 0,
                }}
              />
              <img
                ref={imgRef}
                src={spriteUrl}
                style={{ display: 'none' }}
                onLoad={(e) => setSheetSize({ w: e.currentTarget.naturalWidth, h: e.currentTarget.naturalHeight })}
              />
              <Box width="100%">
                <MuiSlider
                  min={0}
                  max={maxTime}
                  step={0.1}
                  value={sliderValue}
                  onChange={(_, v) => setSliderValue(v as number)}
                  valueLabelDisplay="auto"
                  valueLabelFormat={(v) => {
                    const s = Math.floor(v % 60);
                    const m = Math.floor((v / 60) % 60);
                    const h = Math.floor(v / 3600);
                    return h > 0
                      ? `${h}:${String(m).padStart(2, '0')}:${String(s).padStart(2, '0')}`
                      : `${m}:${String(s).padStart(2, '0')}`;
                  }}
                />
                <Stack direction="row" justifyContent="space-between">
                  <Typography variant="caption" color="text.secondary">0:00</Typography>
                  <Typography variant="caption" color="text.secondary">
                    {Math.floor(maxTime / 60)}:{String(Math.floor(maxTime % 60)).padStart(2, '0')}
                  </Typography>
                </Stack>
              </Box>
            </Stack>
          )}

          {!loading && cues.length === 0 && files !== null && (
            <Typography color="text.secondary">No thumbnails found for this config.</Typography>
          )}
        </Stack>
      </DialogContent>
      <DialogActions>
        <Button onClick={onClose}>Close</Button>
      </DialogActions>
    </Dialog>
  );
}

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
  const [previewJob, setPreviewJob] = useState<ThumbnailsGenerationJobDTO | null>(null);

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
                <TableCell>Preview</TableCell>
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
                  <TableCell>
                    {job.preview && job.status === 'SUCCESS' && (
                      <Button
                        size="small"
                        variant="outlined"
                        startIcon={<PlayCircleOutlineIcon />}
                        onClick={() => setPreviewJob(job)}
                      >
                        Preview
                      </Button>
                    )}
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

      {previewJob && user && accountId && (
        <PreviewDialog
          job={previewJob}
          user={user}
          accountId={accountId}
          onClose={() => setPreviewJob(null)}
        />
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
