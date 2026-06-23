// AS-IS 전용: spring-cloud-gateway-demo(profile=authz) 가 호출하는 세션/인가 검증 서비스.
// TO-BE 에서는 istio-ambient-demo:authz-server(gRPC ext_authz) 가 동일한 역할을 수행한다.
const http = require("http");

const server = http.createServer((req, res) => {
  if (req.url !== "/check") {
    res.writeHead(404, { "Content-Type": "text/plain" });
    res.end("session-svc: 404\n");
    return;
  }

  const demoUser = req.headers["x-demo-user"] || "";
  const rid = req.headers["x-request-id"] || "(none)";

  const allowed =
    demoUser.trim().length > 0 && demoUser.trim().toLowerCase() !== "deny";

  if (allowed) {
    res.writeHead(200, { "Content-Type": "application/json" });
    res.end(JSON.stringify({ user: demoUser.trim(), result: "allow", requestId: rid }));
  } else {
    res.writeHead(403, { "Content-Type": "application/json" });
    res.end(
      JSON.stringify({
        result: "deny",
        reason: "missing or invalid x-demo-user",
        requestId: rid,
      })
    );
  }
});

server.listen(8080, "0.0.0.0", () => console.log("session-svc listening on :8080"));
