// 공통 백엔드 서비스: 수신한 인가 헤더를 그대로 출력해서
// AS-IS(gateway-authz)와 TO-BE(ext_authz)가 동일한 헤더를 전달하는지 비교할 수 있게 한다.
const http = require("http");

const server = http.createServer((req, res) => {
  if (req.url !== "/api") {
    res.writeHead(404, { "Content-Type": "text/plain" });
    res.end("B: 404\n");
    return;
  }

  const user = req.headers["x-demo-user"] || "(none)";
  const authzUser = req.headers["x-authz-user"] || "(none)";
  const authzResult = req.headers["x-authz-result"] || "(none)";
  const rid = req.headers["x-request-id"] || "(none)";

  res.writeHead(200, { "Content-Type": "text/plain" });
  res.end(
    [
      "=== Service B (upstream) ===",
      "x-demo-user: " + user,
      "x-authz-user: " + authzUser,
      "x-authz-result: " + authzResult,
      "x-request-id: " + rid,
      "",
    ].join("\n")
  );
});

server.listen(8080, "0.0.0.0", () => console.log("B server listening on :8080"));
