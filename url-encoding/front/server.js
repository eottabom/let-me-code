/**
 * 프론트엔드 서버 — Node.js 기본 모듈만 사용.
 *
 * 실행: node front/server.js
 * 접속: http://localhost:3000
 *
 * Spring Boot 백엔드(localhost:8080)가 먼저 실행되어 있어야 한다.
 */
const http = require('http');
const fs = require('fs');
const path = require('path');

const PORT = 3000;
const HTML_FILE = path.join(__dirname, 'index.html');

const server = http.createServer((req, res) => {
  fs.readFile(HTML_FILE, (err, data) => {
    if (err) {
      res.writeHead(500);
      res.end('Error loading index.html');
      return;
    }
    res.writeHead(200, { 'Content-Type': 'text/html; charset=utf-8' });
    res.end(data);
  });
});

server.listen(PORT, () => {
  console.log(`Front server: http://localhost:${PORT}`);
  console.log('Back server:  http://localhost:8080');
});
