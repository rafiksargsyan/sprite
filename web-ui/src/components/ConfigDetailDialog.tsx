import { Button, Dialog, DialogActions, DialogContent, DialogTitle, Stack, Typography } from '@mui/material';
import type { ThumbnailConfigResponse } from '../types/api.types';

export function configChipLabel(c: ThumbnailConfigResponse): string {
  if (c.format === 'blurhash') return `blurhash ${c.interval}s`;
  return `${c.format} ${c.resolution}p Q${(c as any).quality} ${c.interval}s`;
}

export function ConfigDetailDialog({ config, onClose }: { config: ThumbnailConfigResponse; onClose: () => void }) {
  const rows: { label: string; value: string | number | boolean }[] = [
    { label: 'Format', value: config.format },
    { label: 'Interval', value: `${config.interval}s` },
    { label: 'Folder', value: config.folderName },
  ];
  if (config.format !== 'blurhash') {
    rows.push({ label: 'Resolution', value: `${config.resolution}p` });
    rows.push({ label: 'Quality', value: (config as any).quality });
    rows.push({ label: 'Sprite rows', value: config.spriteSize!.rows });
    rows.push({ label: 'Sprite cols', value: config.spriteSize!.cols });
  }
  if (config.format === 'blurhash') {
    rows.push({ label: 'Components X', value: (config as any).componentsX });
    rows.push({ label: 'Components Y', value: (config as any).componentsY });
  }
  if (config.format === 'webp') {
    rows.push({ label: 'Preset', value: (config as any).preset });
    rows.push({ label: 'Method', value: (config as any).method });
    rows.push({ label: 'Lossless', value: String((config as any).lossless) });
  }
  if (config.format === 'avif') {
    rows.push({ label: 'Speed', value: (config as any).speed });
  }
  return (
    <Dialog open onClose={onClose} maxWidth="xs" fullWidth>
      <DialogTitle>Config — {config.folderName}</DialogTitle>
      <DialogContent>
        <Stack spacing={1} mt={1}>
          {rows.map(({ label, value }) => (
            <Stack key={label} direction="row" justifyContent="space-between">
              <Typography variant="body2" color="text.secondary">{label}</Typography>
              <Typography variant="body2" fontWeight="medium">{String(value)}</Typography>
            </Stack>
          ))}
        </Stack>
      </DialogContent>
      <DialogActions>
        <Button onClick={onClose}>Close</Button>
      </DialogActions>
    </Dialog>
  );
}
