import { useEffect, useMemo, useState } from "react";
import { FlowApi, type FlowState, type FlowType } from "@/api/flow";
import StepRenderer from "@/steps/StepRenderer.tsx";

export default function AuthFlowPage({ flowType }: { flowType: FlowType }) {
    const [flow, setFlow] = useState<FlowState | null>(null);
    const [err, setErr] = useState("");

    const key = useMemo(() => `authflow:${flowType}`, [flowType]);

    useEffect(() => {
        // 你也可以不缓存：删掉 localStorage 即可
        const cached = localStorage.getItem(key);
        if (cached) {
            try { setFlow(JSON.parse(cached)); } catch { }
        }
        FlowApi.start(flowType)
            .then((s) => {
                setFlow(s);
                localStorage.setItem(key, JSON.stringify(s));
            })
            .catch((e) => setErr(String(e)));
    }, [flowType, key]);

    async function submit(action: string, input?: Record<string, any>) {
        if (!flow) return;
        setErr("");
        try {
            const next = await FlowApi.step(flow.flowId, action, input);
            setFlow(next);
            localStorage.setItem(key, JSON.stringify(next));
        } catch (e) {
            setErr(String(e));
        }
    }

    const title =
        flowType === "LOGIN" ? "Login" :
            flowType === "REGISTER" ? "Register" : "Reset Password";

    return (
        <div style={{ maxWidth: 520, margin: "40px auto", padding: 16 }}>
            <h2 style={{ margin: "0 0 8px" }}>{title}</h2>

            {flow && (
                <div style={{ fontSize: 12, opacity: 0.7, marginBottom: 12 }}>
                    step: <b>{flow.step}</b> · flowId: {flow.flowId}
                </div>
            )}

            {err && <div style={{ color: "red", marginBottom: 12 }}>{err}</div>}
            {!flow ? <div>Loading...</div> : <StepRenderer step={flow.step} data={flow.data ?? {}} onSubmit={submit} />}
            <div style={{ marginTop: 16, fontSize: 13, opacity: 0.7 }}>
                {flowType !== "LOGIN" && <a href="/login">Go to login</a>}{" "}
                {flowType !== "REGISTER" && <a href="/register">Register</a>}{" "}
                {flowType !== "RESET_PASSWORD" && <a href="/reset">Forgot password</a>}
            </div>
        </div>
    );
}