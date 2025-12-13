import { useState } from "react";

export default function VerifyCode({
    data,
    onSubmit,
}: {
    data: Record<string, any>;
    onSubmit: (action: string, input?: any) => void;
}) {
    const [code, setCode] = useState("");

    return (
        <div>
            <div style={{ opacity: 0.7, marginBottom: 8 }}>
                Target: {String(data.identifier ?? data.target ?? "")}
            </div>
            <label>Code</label>
            <input
                value={code}
                onChange={(e) => setCode(e.target.value)}
                placeholder="code"
                style={{ width: "100%", padding: 10, marginTop: 6 }}
            />
            <div style={{ display: "flex", gap: 8, marginTop: 12 }}>
                <button onClick={() => onSubmit("verifyCode", { code })}>Verify</button>
                <button onClick={() => onSubmit("resendCode")}>Resend</button>
            </div>
        </div>
    );
}