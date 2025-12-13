export default function ChooseMfa({
    data,
    onSubmit,
}: {
    data: Record<string, any>;
    onSubmit: (action: string, input?: any) => void;
}) {
    const methods: string[] = data.methods ?? ["TOTP", "SMS"];

    return (
        <div>
            <div style={{ marginBottom: 12 }}>Choose MFA method</div>
            <div style={{ display: "flex", gap: 8, flexWrap: "wrap" }}>
                {methods.map((m) => (
                    <button key={m} onClick={() => onSubmit("selectMfa", { method: m })}>
                        {m}
                    </button>
                ))}
            </div>
        </div>
    );
}