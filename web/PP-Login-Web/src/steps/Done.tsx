export default function Done({ data }: { data: Record<string, any> }) {
    return (
        <div>
            <h3>✅ Success</h3>
            <pre style={{ background: "#f6f6f6", padding: 12 }}>
                {JSON.stringify(data, null, 2)}
            </pre>
        </div>
    );
}