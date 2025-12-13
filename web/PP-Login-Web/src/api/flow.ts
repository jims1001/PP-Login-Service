export type FlowType = "LOGIN" | "REGISTER" | "RESET_PASSWORD";
export type FlowState = {
  flowId: string;
  type: FlowType;
  step: string;
  data?: Record<string, any>;
};

async function json<T>(url: string, init?: RequestInit): Promise<T> {
  const res = await fetch(url, init);
  if (!res.ok)
    throw new Error((await res.text().catch(() => "")) || `${res.status}`);
  return res.json();
}

export const FlowApi = {
  start(type: FlowType) {
    return json<FlowState>("/api/flow/start", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ type }),
    });
  },
  step(flowId: string, action: string, input?: Record<string, any>) {
    return json<FlowState>(`/api/flow/${encodeURIComponent(flowId)}/step`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ action, input }),
    });
  },
  get(flowId: string) {
    return json<FlowState>(`/api/flow/${encodeURIComponent(flowId)}`);
  },
};
