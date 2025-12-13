import { useState } from "react";

export default function EnterPassword({ onSubmit }: { onSubmit: (action: string, input?: any) => void }) {
    const [password, setPassword] = useState("");

    return (
        <div>
            <label>Password</label>
            <input
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                type="password"
                placeholder="password"
                style={{ width: "100%", padding: 10, marginTop: 6 }}
            />
            <button style={{ marginTop: 12 }} onClick={() => onSubmit("login", { password })}>
                Login
            </button>
        </div>
    );
}