import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * SimBroadcaster
 * Drop-in UDP broadcaster for Simulation.java.
 *
 * Usage in your sim loop:
 *
 *   SimBroadcaster bc = new SimBroadcaster("localhost", 9000);
 *   while (true) {
 *       sim.step(86400);
 *       bc.send(sim.getBodies(), sim.getTime());
 *       Thread.sleep(50); // ~20fps to browser
 *   }
 */
public class SimBroadcaster implements AutoCloseable {

    private final DatagramSocket socket;
    private final InetAddress    addr;
    private final int            port;

    private DatagramSocket cmdSocket;

    /** Shared speed multiplier, written by command listener thread */
    public volatile double speedMultiplier = 1.0;
    public volatile boolean resetRequested = false;
    private int stepSkip   = 1;
    private int stepCounter = 0;

    public SimBroadcaster(String host, int port) throws Exception {
        this.socket = new DatagramSocket();
        this.addr   = InetAddress.getByName(host);
        this.port   = port;
        startCommandListener();
    }

    /** Listens on UDP 9002 for speed commands from sim-server.js */
    private void startCommandListener() throws Exception {
        cmdSocket = new DatagramSocket(9002);
        Thread t = new Thread(() -> {
            byte[] buf = new byte[256];
            while (!cmdSocket.isClosed()) {
                try {
                    DatagramPacket pkt = new DatagramPacket(buf, buf.length);
                    cmdSocket.receive(pkt);
                    String json = new String(pkt.getData(), 0, pkt.getLength(), StandardCharsets.UTF_8);
                    // Parse {"type":"setSpeed","multiplier":N}
                    int idx = json.indexOf("\"multiplier\":");
                    if (idx >= 0) {
                        String val = json.substring(idx + 13).replaceAll("[^0-9.eE+-].*", "");
                        double m = Double.parseDouble(val);
                        speedMultiplier = Math.max(0.001, Math.min(1000.0, m));
                        System.out.printf("[CMD] speed set to %.3fx%n", speedMultiplier);
                    }
                } catch (Exception e) {
                    if (!cmdSocket.isClosed())
                        System.err.println("[CMD] error: " + e.getMessage());
                }
            }
        }, "cmd-listener");
        t.setDaemon(true);
        t.start();
    }

    /** Every n-th step to send (throttle if your sim runs faster than 20fps) */

    /**
     * Call once per sim.step(). Throttles automatically via stepSkip.
     */
    public void send(List<Body> bodies, double simTime) {
        if (++stepCounter % stepSkip != 0) return;
        try {
            String json = buildJson(bodies, simTime);
            byte[] data = json.getBytes(StandardCharsets.UTF_8);
            // UDP has a ~65KB limit; for huge body counts split into multiple packets
            if (data.length > 60_000) {
                sendChunked(bodies, simTime);
                return;
            }
            socket.send(new DatagramPacket(data, data.length, addr, port));
        } catch (Exception e) {
            System.err.println("[SimBroadcaster] send error: " + e.getMessage());
        }
    }

    private void sendChunked(List<Body> bodies, double simTime) throws Exception {
        int chunk = 20;
        for (int i = 0; i < bodies.size(); i += chunk) {
            List<Body> sub = bodies.subList(i, Math.min(i + chunk, bodies.size()));
            String json = buildJson(sub, simTime);
            byte[] data = json.getBytes(StandardCharsets.UTF_8);
            socket.send(new DatagramPacket(data, data.length, addr, port));
        }
    }

    private String buildJson(List<Body> bodies, double simTime) {
        StringBuilder sb = new StringBuilder(512);
        sb.append("{\"type\":\"frame\",\"time\":").append(simTime)
          .append(",\"bodies\":[");
        for (int i = 0; i < bodies.size(); i++) {
            if (i > 0) sb.append(',');
            appendBody(sb, bodies.get(i));
        }
        sb.append("]}");
        return sb.toString();
    }

    private void appendBody(StringBuilder sb, Body b) {
        sb.append('{');
        appendKV(sb, "name",   b.getName(),   true);  sb.append(',');
        appendKV(sb, "x",      b.getX());              sb.append(',');
        appendKV(sb, "y",      b.getY());              sb.append(',');
        appendKV(sb, "z",      b.getZ());              sb.append(',');
        appendKV(sb, "vx",     b.getVelocityX());      sb.append(',');
        appendKV(sb, "vy",     b.getVelocityY());      sb.append(',');
        appendKV(sb, "vz",     b.getVelocityZ());      sb.append(',');
        appendKV(sb, "mass",   b.getMass());            sb.append(',');
        appendKV(sb, "radius", b.getRadius());

        // Parent body name for orbit ring drawing
        if (b.getOrbits() != null) {
            sb.append(",\"parent\":\"").append(b.getOrbits().getName()).append('"');
        }

        // Star-specific extras (spectral class, temperature)
        if (b instanceof Star) {
            Star s = (Star) b;
            sb.append(",\"spectral\":\"").append(s.getSpectralClass()).append('"');
            sb.append(",\"temp\":").append(s.getTemperature());
            sb.append(",\"luminosity\":").append(s.getLuminosity());
        }
        sb.append('}');
    }

    private void appendKV(StringBuilder sb, String k, String v, boolean quoted) {
        sb.append('"').append(k).append("\":\"").append(v).append('"');
    }
    private void appendKV(StringBuilder sb, String k, double v) {
        sb.append('"').append(k).append("\":").append(Double.isFinite(v) ? v : 0.0);
    }
    private void appendKV(StringBuilder sb, String k, Double v) {
        appendKV(sb, k, v != null ? v : 0.0);
    }

    public void setStepSkip(int n) { this.stepSkip = Math.max(1, n); }

    @Override
    public void close() {
        socket.close();
        if (cmdSocket != null) cmdSocket.close();
    }
}