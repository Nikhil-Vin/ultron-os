// Render-event types — mirror voice/contract.md (v1).

export type VoiceState = "idle" | "listening" | "thinking" | "speaking" | "interrupted";

export interface MetricPoint { label: string; value: number; }
export interface PipelineStage { label: string; value: number; atRisk?: boolean; }
export interface IntelItem { when: string; text: string; }
export interface ActionItem { text: string; done: boolean; }

export interface RenderEvent {
  type:
    | "state"
    | "transcript"
    | "sentence"
    | "brief"
    | "metrics"
    | "pipeline"
    | "intel"
    | "actions";
  ts?: number;
  topic?: string;
  payload: any;
}
