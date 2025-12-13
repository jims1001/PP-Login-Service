await fetch("/api/register", {
  method: "POST",
  headers: { "Content-Type": "application/json" },
  body: JSON.stringify({
    tenantId: "10000",
    clientId: "pp-login-web",
    ip: "127.0.0.1",
    ua: navigator.userAgent,
    deviceFingerprint: "fp_xxx",

    identifierType: "EMAIL",
    identifier: "user@example.com",
    displayName: "Test User",
    password: "P@ssw0rd123",
  }),
});
