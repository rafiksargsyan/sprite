import { decode as decodeBlurhash } from 'blurhash';
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
  IconButton,
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
import DeleteIcon from '@mui/icons-material/Delete';
import CancelIcon from '@mui/icons-material/Cancel';
import DownloadIcon from '@mui/icons-material/Download';
import PlayCircleOutlineIcon from '@mui/icons-material/PlayCircleOutline';
import { useAuth } from '../hooks/useAuth';
import { listJobs, createJob, cancelJob, getJobLimits, getJobPreviewFiles, getJobPreviewVtt } from '../api/jobs';
import { listJobSpecs } from '../api/jobSpecs';
import type { ThumbnailsGenerationJobDTO, JobSpecDTO, PreviewFilesResponse, ThumbnailConfigResponse, ThumbnailConfigRequest } from '../types/api.types';
import { configChipLabel, ConfigDetailDialog } from '../components/ConfigDetailDialog';

const fmt = (ts: string | null) =>
  ts ? new Date(ts).toLocaleString() : '—';

type SpriteCue = {
  type: 'sprite';
  start: number;
  end: number;
  file: string;
  x: number;
  y: number;
  w: number;
  h: number;
};

type BlurhashCue = {
  type: 'blurhash';
  start: number;
  end: number;
  hash: string;
  w: number;
  h: number;
};

type VttCue = SpriteCue | BlurhashCue;

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
      const start = parseTimestamp(match[1]);
      const end = parseTimestamp(match[2]);
      const payload = lines[++i]?.trim() ?? '';
      const spriteMatch = payload.match(/^(.+?)#xywh=(\d+),(\d+),(\d+),(\d+)$/);
      const blurhashMatch = !spriteMatch ? payload.match(/^(\S+)\s+(\d+)\s+(\d+)$/) : null;
      if (spriteMatch) {
        cues.push({ type: 'sprite', start, end, file: spriteMatch[1], x: parseInt(spriteMatch[2]), y: parseInt(spriteMatch[3]), w: parseInt(spriteMatch[4]), h: parseInt(spriteMatch[5]) });
      } else if (blurhashMatch) {
        cues.push({ type: 'blurhash', start, end, hash: blurhashMatch[1], w: parseInt(blurhashMatch[2]), h: parseInt(blurhashMatch[3]) });
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
    Promise.all([
      getJobPreviewFiles(user, accountId, job.id, selectedConfig),
      getJobPreviewVtt(user, accountId, job.id, selectedConfig),
    ])
      .then(([f, vtt]) => {
        setFiles(f);
        setCues(parseVtt(vtt));
        setSheetSize(null);
      })
      .finally(() => setLoading(false));
  }, [selectedConfig]);

  const canvasRef = useRef<HTMLCanvasElement>(null);
  const maxTime = cues.length > 0 ? cues[cues.length - 1].end : 0;
  const cue = findCue(cues, sliderValue);
  const scale = cue ? displayWidth / cue.w : 1;
  const displayHeight = cue ? Math.round(cue.h * scale) : 0;
  const spriteUrl = cue?.type === 'sprite' && files ? files[cue.file] : null;

  useEffect(() => {
    if (!canvasRef.current || cue?.type !== 'blurhash') return;
    const pixels = decodeBlurhash(cue.hash, displayWidth, displayHeight);
    const ctx = canvasRef.current.getContext('2d');
    if (!ctx) return;
    const imageData = new ImageData(pixels as unknown as Uint8ClampedArray<ArrayBuffer>, displayWidth, displayHeight);
    ctx.putImageData(imageData, 0, 0);
  }, [cue, displayWidth, displayHeight]);

  return (
    <Dialog open onClose={onClose} maxWidth="md" fullWidth>
      <DialogTitle>Preview — {job.id.slice(0, 12)}…</DialogTitle>
      <DialogContent>
        <Stack spacing={3} mt={1} sx={{ position: 'relative' }}>
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
                max={1280}
                step={10}
                value={displayWidth}
                onChange={(_, v) => setDisplayWidth(v as number)}
                size="small"
              />
            </Box>
          </Stack>

          {loading && cue && (
            <Box display="flex" justifyContent="center" alignItems="center" sx={{ position: 'absolute', inset: 0, zIndex: 1 }}>
              <CircularProgress />
            </Box>
          )}

          {!loading && !cue && <Box display="flex" justifyContent="center"><CircularProgress /></Box>}

          {cue && (
            <Stack spacing={1} alignItems="center" sx={{ opacity: loading ? 0.4 : 1, transition: 'opacity 0.2s' }}>
              <Box sx={{ position: 'relative', display: 'inline-block', mt: '20px', mr: '48px' }}>
                {/* Width label — top */}
                <Typography variant="caption" color="text.secondary" sx={{
                  position: 'absolute', top: -18, left: 0, right: 0,
                  textAlign: 'center', fontSize: '0.7rem', lineHeight: 1,
                }}>
                  {displayWidth}px
                </Typography>
                {/* Height label — right, rotated */}
                <Typography variant="caption" color="text.secondary" sx={{
                  position: 'absolute', right: -44, top: '50%',
                  transform: 'translateY(-50%) rotate(90deg)',
                  whiteSpace: 'nowrap', fontSize: '0.7rem', lineHeight: 1,
                }}>
                  {displayHeight}px
                </Typography>
                {cue.type === 'blurhash' ? (
                  <canvas
                    ref={canvasRef}
                    width={displayWidth}
                    height={displayHeight}
                    style={{ border: '1px solid', borderColor: 'divider', display: 'block' }}
                  />
                ) : (
                  <Box
                    sx={{
                      width: displayWidth,
                      height: displayHeight,
                      backgroundImage: spriteUrl ? `url(${spriteUrl})` : 'none',
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
                )}
              </Box>
              {cue.type !== 'blurhash' && (
                <img
                  ref={imgRef}
                  src={spriteUrl ?? undefined}
                  style={{ display: 'none' }}
                  onLoad={(e) => setSheetSize({ w: e.currentTarget.naturalWidth, h: e.currentTarget.naturalHeight })}
                />
              )}
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

          {!loading && cues.length === 0 && (
            <Typography color="text.secondary" display="flex" justifyContent="center">No thumbnails found for this config.</Typography>
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
  QUEUED: 'info',
  RECEIVED: 'info',
  IN_PROGRESS: 'warning',
  RETRYING: 'warning',
  SUCCESS: 'success',
  FAILURE: 'error',
  CANCELLED: 'default',
};

const TERMINAL_STATUSES = new Set<ThumbnailsGenerationJobDTO['status']>(['SUCCESS', 'FAILURE', 'CANCELLED']);

const FAILURE_REASON_LABEL: Record<import('../types/api.types').JobFailureReason, string> = {
  VIDEO_TOO_LARGE: 'Video too large',
  VIDEO_NOT_ACCESSIBLE: 'Video not accessible',
  INVALID_STREAM_INDEX: 'Invalid stream index',
  SERVER_ERROR: 'Server error',
};

interface ConfigDraft {
  format: 'jpg' | 'webp' | 'avif' | 'blurhash';
  resolution: number;
  spriteRows: number;
  spriteCols: number;
  quality: number;
  interval: number;
  method: number;
  componentsX: number;
  componentsY: number;
  folderName: string;
}

const defaultConfig = (): ConfigDraft => ({
  format: 'jpg', resolution: 120, spriteRows: 10, spriteCols: 10,
  quality: 85, interval: 10, method: 4, componentsX: 4, componentsY: 3, folderName: '',
});

function configDraftToRequest(c: ConfigDraft): ThumbnailConfigRequest {
  const spriteSize = { rows: c.spriteRows, cols: c.spriteCols };
  if (c.format === 'jpg') return { format: 'jpg', resolution: c.resolution, spriteSize, quality: c.quality, interval: c.interval, folderName: c.folderName };
  if (c.format === 'avif') return { format: 'avif', resolution: c.resolution, spriteSize, quality: c.quality, interval: c.interval, folderName: c.folderName };
  if (c.format === 'blurhash') return { format: 'blurhash', interval: c.interval, componentsX: c.componentsX, componentsY: c.componentsY, folderName: c.folderName };
  return { format: 'webp', resolution: c.resolution, spriteSize, quality: c.quality, interval: c.interval, method: c.method, folderName: c.folderName };
}

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
  const [specMode, setSpecMode] = useState<'existing' | 'custom'>('existing');
  const [jobSpecId, setJobSpecId] = useState('');
  const [configs, setConfigs] = useState<ConfigDraft[]>([defaultConfig()]);
  const [streamIndex, setStreamIndex] = useState('');
  const [preview, setPreview] = useState(false);
  const [maxFileSizeBytes, setMaxFileSizeBytes] = useState<number | null>(null);
  const [previewJob, setPreviewJob] = useState<ThumbnailsGenerationJobDTO | null>(null);
  const [selectedConfig, setSelectedConfig] = useState<ThumbnailConfigResponse | null>(null);
  const [cancellingId, setCancellingId] = useState<string | null>(null);

  useEffect(() => {
    getJobLimits().then((l) => setMaxFileSizeBytes(l.maxFileSizeBytes)).catch(() => {});
  }, []);

  useEffect(() => {
    if (!user || !accountId) return;
    setLoading(true);
    Promise.all([listJobs(user, accountId, page, pageSize), listJobSpecs(user, accountId)])
      .then(([j, s]) => { setJobs(j.content); setTotalElements(j.page.totalElements); setSpecs(s); })
      .catch(() => setError('Failed to load data'))
      .finally(() => setLoading(false));
  }, [user, accountId, page, pageSize]);

  const addConfig = () => setConfigs((prev) => [...prev, defaultConfig()]);
  const removeConfig = (i: number) => setConfigs((prev) => prev.filter((_, idx) => idx !== i));
  const updateConfig = (i: number, patch: Partial<ConfigDraft>) =>
    setConfigs((prev) => prev.map((c, idx) => (idx === i ? { ...c, ...patch } : c)));

  const openDialog = () => {
    setVideoURL('');
    setSpecMode(specs.length > 0 ? 'existing' : 'custom');
    setJobSpecId(specs[0]?.id ?? '');
    setConfigs([defaultConfig()]);
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
      const body = specMode === 'existing'
        ? { videoURL, jobSpecId, streamIndex: parsedStreamIndex, preview }
        : { videoURL, configs: configs.map(configDraftToRequest), streamIndex: parsedStreamIndex, preview };
      await createJob(user, accountId, body);
      setPage(0);
      const refreshed = await listJobs(user, accountId, 0, pageSize);
      setJobs(refreshed.content);
      setTotalElements(refreshed.page.totalElements);
      setDialogOpen(false);
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Failed to create job');
    } finally {
      setSaving(false);
    }
  };

  const handleCancel = async (jobId: string) => {
    if (!user || !accountId) return;
    setCancellingId(jobId);
    try {
      await cancelJob(user, accountId, jobId);
      setJobs((prev) => prev.map((j) => j.id === jobId ? { ...j, status: 'CANCELLED' } : j));
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Failed to cancel job');
    } finally {
      setCancellingId(null);
    }
  };

  return (
    <Box>
      <Stack direction="row" justifyContent="space-between" alignItems="center" mb={3}>
        <Typography variant="h5" fontWeight="bold">Jobs</Typography>
        <Button variant="contained" color="secondary" startIcon={<AddIcon />} onClick={openDialog}>
          New Job
        </Button>
      </Stack>

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
                <TableCell>Cost</TableCell>
                <TableCell>Download</TableCell>
                <TableCell>Preview</TableCell>
                <TableCell></TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {jobs.map((job) => (
                <TableRow key={job.id} hover>
                  <TableCell>
                    <Typography variant="body2" fontFamily="monospace">{job.id}</Typography>
                  </TableCell>
                  <TableCell>
                    <Typography variant="body2">{job.videoUrl}</Typography>
                  </TableCell>
                  <TableCell>
                    <Stack direction="row" gap={0.5} flexWrap="wrap">
                      {job.jobSpec.configs.map((c, i) => (
                        <Chip key={i} size="small" label={configChipLabel(c)} onClick={() => setSelectedConfig(c)} sx={{ cursor: 'pointer' }} />
                      ))}
                    </Stack>
                  </TableCell>
                  <TableCell>
                    <Tooltip title={job.failureReason ? FAILURE_REASON_LABEL[job.failureReason] : ''}>
                      <Chip
                        size="small"
                        label={job.status}
                        color={STATUS_COLOR[job.status]}
                      />
                    </Tooltip>
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
                    <Typography variant="body2">
                      {job.cost != null ? Math.round(job.cost) : '—'}
                    </Typography>
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
                    {job.preview && (
                      <Tooltip title={
                        job.status === 'FAILURE'
                          ? 'Job failed'
                          : job.status !== 'SUCCESS'
                            ? 'Job not finished yet'
                            : !job.previewAvailable
                              ? 'Preview expired (available for 2 hours after completion)'
                              : ''
                      }>
                        <span>
                          <Button
                            size="small"
                            variant="outlined"
                            startIcon={<PlayCircleOutlineIcon />}
                            onClick={() => setPreviewJob(job)}
                            disabled={!job.previewAvailable}
                          >
                            Preview
                          </Button>
                        </span>
                      </Tooltip>
                    )}
                  </TableCell>
                  <TableCell>
                    {!TERMINAL_STATUSES.has(job.status) && (
                      <Tooltip title="Cancel job">
                        <span>
                          <Button
                            size="small"
                            variant="outlined"
                            color="error"
                            startIcon={cancellingId === job.id ? <CircularProgress size={14} /> : <CancelIcon />}
                            onClick={() => handleCancel(job.id)}
                            disabled={cancellingId === job.id}
                          >
                            Cancel
                          </Button>
                        </span>
                      </Tooltip>
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

      {selectedConfig && <ConfigDetailDialog config={selectedConfig} onClose={() => setSelectedConfig(null)} />}

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
            <FormControl fullWidth size="small">
              <InputLabel>Spec source</InputLabel>
              <Select
                label="Spec source"
                value={specMode}
                onChange={(e) => setSpecMode(e.target.value as 'existing' | 'custom')}
              >
                <MenuItem value="existing" disabled={specs.length === 0}>Existing job spec</MenuItem>
                <MenuItem value="custom">Custom (inline)</MenuItem>
              </Select>
            </FormControl>

            {specMode === 'existing' && (
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
            )}

            {specMode === 'custom' && (
              <Stack spacing={1.5}>
                {configs.map((cfg, i) => (
                  <Stack key={i} spacing={1} sx={{ p: 1.5, border: '1px solid', borderColor: 'divider', borderRadius: 1 }}>
                    <Stack direction="row" alignItems="center" justifyContent="space-between">
                      <Typography variant="body2" fontWeight="medium">Config {i + 1}</Typography>
                      <IconButton size="small" onClick={() => removeConfig(i)} disabled={configs.length === 1}>
                        <DeleteIcon fontSize="small" />
                      </IconButton>
                    </Stack>
                    <Stack direction="row" spacing={1} flexWrap="wrap" useFlexGap>
                      <TextField size="small" label="Folder name" value={cfg.folderName}
                        onChange={(e) => updateConfig(i, { folderName: e.target.value })} sx={{ width: 130 }} />
                      <FormControl size="small" sx={{ width: 110 }}>
                        <InputLabel>Format</InputLabel>
                        <Select label="Format" value={cfg.format}
                          onChange={(e) => updateConfig(i, { format: e.target.value as ConfigDraft['format'], quality: e.target.value === 'jpg' ? 85 : 60 })}>
                          <MenuItem value="jpg">JPG</MenuItem>
                          <MenuItem value="webp">WebP</MenuItem>
                          <MenuItem value="avif">AVIF</MenuItem>
                          <MenuItem value="blurhash">Blurhash</MenuItem>
                        </Select>
                      </FormControl>
                      <TextField size="small" label="Interval (s)" type="number" value={cfg.interval}
                        onChange={(e) => updateConfig(i, { interval: +e.target.value })} sx={{ width: 100 }} />
                      {cfg.format !== 'blurhash' && <>
                        <TextField size="small" label="Resolution (px)" type="number" value={cfg.resolution}
                          onChange={(e) => updateConfig(i, { resolution: +e.target.value })} sx={{ width: 130 }} />
                        <TextField size="small" label="Quality" type="number" value={cfg.quality}
                          onChange={(e) => updateConfig(i, { quality: +e.target.value })} sx={{ width: 80 }} />
                        <TextField size="small" label="Rows" type="number" value={cfg.spriteRows}
                          onChange={(e) => updateConfig(i, { spriteRows: +e.target.value })} sx={{ width: 70 }} />
                        <TextField size="small" label="Cols" type="number" value={cfg.spriteCols}
                          onChange={(e) => updateConfig(i, { spriteCols: +e.target.value })} sx={{ width: 70 }} />
                      </>}
                      {cfg.format === 'webp' && (
                        <TextField size="small" label="Method (0-6)" type="number" value={cfg.method}
                          onChange={(e) => updateConfig(i, { method: +e.target.value })} sx={{ width: 110 }} />
                      )}
                      {cfg.format === 'blurhash' && <>
                        <TextField size="small" label="Components X" type="number" value={cfg.componentsX}
                          onChange={(e) => updateConfig(i, { componentsX: +e.target.value })} sx={{ width: 120 }} />
                        <TextField size="small" label="Components Y" type="number" value={cfg.componentsY}
                          onChange={(e) => updateConfig(i, { componentsY: +e.target.value })} sx={{ width: 120 }} />
                      </>}
                    </Stack>
                  </Stack>
                ))}
                <Button size="small" startIcon={<AddIcon />} onClick={addConfig} disabled={configs.length >= 5}>
                  Add Config
                </Button>
              </Stack>
            )}
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
            disabled={saving || !videoURL.trim() || (specMode === 'existing' && !jobSpecId)}
          >
            {saving ? <CircularProgress size={20} /> : 'Submit'}
          </Button>
        </DialogActions>
      </Dialog>
    </Box>
  );
}
