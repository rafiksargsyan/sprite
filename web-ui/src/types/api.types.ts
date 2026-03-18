export interface ApiKeyDTO {
  id: string;
  key: string | null;
  lastAccessTime: string | null;
  description: string;
  disabled: boolean;
}

export interface UserDTO {
  id: string;
  name: string;
  accountId: string;
}

export interface SpriteSizeResponse {
  rows: number;
  cols: number;
}

export interface JpgConfigResponse {
  format: 'jpg';
  resolution: number;
  spriteSize: SpriteSizeResponse;
  quality: number;
  interval: number;
  folderName: string;
}

export interface WebpConfigResponse {
  format: 'webp';
  resolution: number;
  spriteSize: SpriteSizeResponse;
  quality: number;
  method: number;
  lossless: boolean;
  interval: number;
  preset: string;
  folderName: string;
}

export interface AvifConfigResponse {
  format: 'avif';
  resolution: number;
  spriteSize: SpriteSizeResponse;
  quality: number;
  interval: number;
  speed: number;
  folderName: string;
}

export interface BlurhashConfigResponse {
  format: 'blurhash';
  resolution: number;
  interval: number;
  componentsX: number;
  componentsY: number;
  folderName: string;
}

export type ThumbnailConfigResponse = JpgConfigResponse | WebpConfigResponse | AvifConfigResponse | BlurhashConfigResponse;

export interface JobSpecDTO {
  id: string;
  name: string;
  description?: string;
  configs: ThumbnailConfigResponse[];
}

export interface SpriteSizeRequest {
  rows: number;
  cols: number;
}

export interface JpgConfigRequest {
  format: 'jpg';
  resolution: number;
  spriteSize: SpriteSizeRequest;
  quality: number;
  interval: number;
  folderName: string;
}

export interface WebpConfigRequest {
  format: 'webp';
  resolution: number;
  spriteSize: SpriteSizeRequest;
  quality: number;
  method: number;
  lossless: boolean;
  interval: number;
  preset: string;
  folderName: string;
}

export interface AvifConfigRequest {
  format: 'avif';
  resolution: number;
  spriteSize: SpriteSizeRequest;
  quality: number;
  interval: number;
  speed: number;
  folderName: string;
}

export interface BlurhashConfigRequest {
  format: 'blurhash';
  resolution: number;
  interval: number;
  componentsX: number;
  componentsY: number;
  folderName: string;
}

export type ThumbnailConfigRequest = JpgConfigRequest | WebpConfigRequest | AvifConfigRequest | BlurhashConfigRequest;

export interface JobSpecCreationRequest {
  name: string;
  description?: string;
  configs: ThumbnailConfigRequest[];
}

export type JobFailureReason =
  | 'VIDEO_TOO_LARGE'
  | 'VIDEO_NOT_ACCESSIBLE'
  | 'INVALID_STREAM_INDEX'
  | 'SERVER_ERROR';

export interface ThumbnailsGenerationJobDTO {
  id: string;
  videoUrl: string;
  status: 'SUBMITTED' | 'IN_PROGRESS' | 'SUCCESS' | 'FAILURE';
  jobSpec: { configs: ThumbnailConfigResponse[] };
  streamIndex: number | null;
  preview: boolean;
  createdAt: string;
  startedAt: string | null;
  finishedAt: string | null;
  downloadUrl: string | null;
  failureReason: JobFailureReason | null;
}

export interface ThumbnailsGenerationJobCreationRequest {
  videoURL: string;
  jobSpecId: string;
  streamIndex?: number | null;
  preview?: boolean;
}

export interface JobLimitsDTO {
  maxFileSizeBytes: number;
}

export type PreviewFilesResponse = Record<string, string>;

export interface PageResponse<T> {
  content: T[];
  page: {
    totalElements: number;
    totalPages: number;
    number: number;
    size: number;
  };
}
