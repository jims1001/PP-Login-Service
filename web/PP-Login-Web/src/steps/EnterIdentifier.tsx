import { useState } from "react";

export default function EnterIdentifier({ onSubmit }: { onSubmit: (action: string, input?: any) => void }) {
    const [identifier, setIdentifier] = useState("");

    return (
        <div>
            <label>Identifier</label>
            <input
                value={identifier}
                onChange={(e) => setIdentifier(e.target.value)}
                placeholder="email / phone / username"
                style={{ width: "100%", padding: 10, marginTop: 6 }}
            />
            <div style={{ display: "flex", gap: 8, marginTop: 12 }}>
                <button onClick={() => onSubmit("next", { identifier })}>Next</button>
                <button onClick={() => onSubmit("sendCode", { identifier })}>Send Code</button>
            </div>
            <div style={{ fontSize: 12, opacity: 0.7, marginTop: 8 }}>
                * 这里 action 名随你后端：比如后端是 “submitIdentifier” 就改成那个
            </div>
        </div>
    );
}