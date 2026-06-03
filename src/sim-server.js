/**
 * sim-server.js
 * Receives UDP frames from the Java simulation,
 * rebroadcasts over WebSocket to the browser.
 * Also forwards speed-change commands from browser → Java via UDP.
 *
 * Usage:  node sim-server.js
 * Deps:   npm install ws
 */

const dgram     = require('dgram');
const http      = require('http');
const WebSocket = require('ws');

const UDP_PORT      = 9000;   // Java sends sim frames here
const UDP_CMD_PORT  = 9002;   // sim-server sends commands to Java here
const WS_PORT       = 9001;   // Browser connects here

// ── WebSocket server ───────────────────────────────────────────────────────
const httpServer = http.createServer();
const wss = new WebSocket.Server({ server: httpServer });

let clientCount = 0;
wss.on('connection', ws => {
  clientCount++;
  console.log(`[WS] client connected (${clientCount} total)`);

  ws.on('message', (raw) => {
    try {
      const msg = JSON.parse(raw);
      if (msg.type === 'setSpeed' || msg.type === 'reset') {
        const cmd = Buffer.from(JSON.stringify(msg));
        cmdSocket.send(cmd, UDP_CMD_PORT, '127.0.0.1', (err) => {
          if (err) console.error('[CMD] send error:', err.message);
          else console.log(`[CMD] ${msg.type === 'reset' ? 'RESET' : `speed → ${msg.multiplier.toFixed(3)}x`}`);
        });
      }
    } catch(e) {}
  });

  ws.on('close', () => { clientCount--; });
});

function broadcast(msg) {
  const str = typeof msg === 'string' ? msg : JSON.stringify(msg);
  wss.clients.forEach(c => {
    if (c.readyState === WebSocket.OPEN) c.send(str);
  });
}

httpServer.listen(WS_PORT, () => {
  console.log(`[WS]  listening on ws://localhost:${WS_PORT}`);
});

// ── UDP: receive sim frames from Java ─────────────────────────────────────
const udp = dgram.createSocket('udp4');
udp.on('message', (buf) => {
  try {
    const msg = JSON.parse(buf.toString('utf8'));
    if (clientCount > 0) broadcast(msg);
  } catch (e) {
    console.error('[UDP] bad packet:', e.message);
  }
});
udp.bind(UDP_PORT, () => {
  console.log(`[UDP] listening on udp://localhost:${UDP_PORT}`);
});

// ── UDP: send commands to Java ─────────────────────────────────────────────
const cmdSocket = dgram.createSocket('udp4');

console.log(`
  sim-server running
  ─────────────────
  Java  → UDP  :${UDP_PORT}   (sim frames)
  Java  ← UDP  :${UDP_CMD_PORT}   (speed commands)
  Browser ← WS :${WS_PORT}
`);