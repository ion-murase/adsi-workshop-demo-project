import http from "node:http";
import net from "node:net";

const TARGET_PORT = 3001;
const LISTEN_PORT = 3000;
const BASE_PATH = "/ports/3000";

function addBasePath(url) {
  return url.startsWith(BASE_PATH) ? url : BASE_PATH + url;
}

const server = http.createServer((req, res) => {
  const upstream = http.request(
    {
      hostname: "localhost",
      port: TARGET_PORT,
      path: addBasePath(req.url),
      method: req.method,
      headers: { ...req.headers, host: `localhost:${TARGET_PORT}` },
    },
    (proxyRes) => {
      res.writeHead(proxyRes.statusCode, proxyRes.headers);
      proxyRes.pipe(res);
    },
  );

  upstream.on("error", () => {
    res.writeHead(502);
    res.end("Proxy error: Next.js not ready yet");
  });

  req.pipe(upstream);
});

// WebSocket support for HMR
server.on("upgrade", (req, socket, head) => {
  const path = addBasePath(req.url);

  const proxySocket = net.connect(TARGET_PORT, "localhost", () => {
    const headers = Object.entries(req.headers)
      .map(([k, v]) => `${k}: ${v}`)
      .join("\r\n");

    proxySocket.write(
      `${req.method} ${path} HTTP/${req.httpVersion}\r\n${headers}\r\n\r\n`,
    );

    if (head.length > 0) proxySocket.write(head);

    socket.pipe(proxySocket);
    proxySocket.pipe(socket);
  });

  proxySocket.on("error", () => socket.end());
  socket.on("error", () => proxySocket.end());
});

server.listen(LISTEN_PORT, () => {
  console.log(
    `SageMaker proxy: :${LISTEN_PORT} → :${TARGET_PORT} (prepending ${BASE_PATH})`,
  );
});
