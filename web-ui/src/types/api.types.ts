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
}

export interface WebpConfigResponse {
  format: 'webp';
  resolution: number;
  spriteSize: SpriteSizeResponse;
  quality: number;
  method: number;
  lossless: boolean;
  interval: number;
}

export type ThumbnailConfigResponse = JpgConfigResponse | WebpConfigResponse;

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
}

export interface WebpConfigRequest {
  format: 'webp';
  resolution: number;
  spriteSize: SpriteSizeRequest;
  quality: number;
  method: number;
  lossless: boolean;
  interval: number;
}

export type ThumbnailConfigRequest = JpgConfigRequest | WebpConfigRequest;

export interface JobSpecCreationRequest {
  name: string;
  description?: string;
  configs: ThumbnailConfigRequest[];
}

export interface ThumbnailsGenerationJobDTO {
  id: string;
  videoUrl: string;
  status: 'SUBMITTED' | 'QUEUED' | 'IN_PROGRESS' | 'SUCCESS' | 'FAILURE';
  jobSpec: { configs: ThumbnailConfigResponse[] };
}

export interface ThumbnailsGenerationJobCreationRequest {
  videoURL: string;
  jobSpecId: string;
}
