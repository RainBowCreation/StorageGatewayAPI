package net.rainbowcreation.storage.api.network;

import net.rainbowcreation.storage.api.ModelField;
import net.rainbowcreation.storage.api.proxy.ProxyMessenger;

import java.io.*;

import java.net.Socket;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A lightweight TCP Client that handles the StorageGatewayAPI protocol.
 */
public class SimpleTcpMessenger implements ProxyMessenger {
    protected final String host;
    protected final int port;
    protected Socket socket;
    protected DataOutputStream out;
    protected DataInputStream in;
    protected boolean running = true;
    private Thread listenerThread;

    // --- Callbacks for pending requests ---
    protected final Map<String, CompletableFuture<Optional<String>>> pendingGet = new ConcurrentHashMap<>();
    protected final Map<String, CompletableFuture<Void>> pendingSet = new ConcurrentHashMap<>();
    protected final Map<String, CompletableFuture<Void>> pendingDelete = new ConcurrentHashMap<>();
    protected final Map<String, CompletableFuture<Optional<Integer>>> pendingCount = new ConcurrentHashMap<>();

    public SimpleTcpMessenger(String host, int port) {
        this.host = host;
        this.port = port;
    }

    @Override
    public void register() {
        connect();
        // Start background listener
        listenerThread = new Thread(this::listen, "SGW-TCP-Client");
        listenerThread.setDaemon(true); // Allow JVM to exit if this is the only thread left
        listenerThread.start();
    }

    @Override
    public void unregister() {
        running = false;
        disconnect();
        if (listenerThread != null) listenerThread.interrupt();
    }

    protected void connect() {
        try {
            socket = new Socket(host, port);
            out = new DataOutputStream(socket.getOutputStream());
            in = new DataInputStream(socket.getInputStream());
            logInfo("Connected to Master at " + host + ":" + port);
        } catch (IOException e) {
            logSevere("Connection failed: " + e.getMessage());
        }
    }

    protected void disconnect() {
        try {
            if (out != null) out.close();
            if (in != null) in.close();
            if (socket != null) socket.close();
            logInfo("Disconnected.");
        } catch (Exception ignored) {}
    }

    // --- Protocol Implementation ---

    @Override
    public CompletableFuture<Optional<String>> get(String db, String secret, String ns, String key) {
        CompletableFuture<Optional<String>> f = new CompletableFuture<>();
        String reqId = UUID.randomUUID().toString();
        pendingGet.put(reqId, f);

        try {
            ByteArrayOutputStream bout = new ByteArrayOutputStream();
            DataOutputStream out = new DataOutputStream(bout);

            writeHeader(out, "GET", reqId, db, secret, ns);
            out.writeUTF(key);

            sendRaw(bout.toByteArray());
        } catch (Throwable t) {
            pendingGet.remove(reqId);
            f.completeExceptionally(t);
        }
        return f;
    }

    @Override
    public CompletableFuture<Optional<String>> get(String db, String secret, String ns, Map<String, String> filters, Map<String, String> selections, int limit, int offset) {
        CompletableFuture<Optional<String>> f = new CompletableFuture<>();
        String reqId = UUID.randomUUID().toString();
        pendingGet.put(reqId, f);

        try {
            ByteArrayOutputStream bout = new ByteArrayOutputStream();
            DataOutputStream out = new DataOutputStream(bout);

            writeHeader(out, "SEARCH", reqId, db, secret, ns);
            writeMap(out, filters);
            writeMap(out, selections);
            out.writeInt(limit);
            out.writeInt(offset);

            sendRaw(bout.toByteArray());
        } catch (Throwable t) {
            pendingGet.remove(reqId);
            f.completeExceptionally(t);
        }
        return f;
    }

    @Override
    public CompletableFuture<Optional<Integer>> count(String db, String secret, String ns, Map<String, String> filters, int limit, int offset) {
        CompletableFuture<Optional<Integer>> f = new CompletableFuture<>();
        String reqId = UUID.randomUUID().toString();
        pendingCount.put(reqId, f);

        try {
            ByteArrayOutputStream bout = new ByteArrayOutputStream();
            DataOutputStream out = new DataOutputStream(bout);

            writeHeader(out, "COUNT", reqId, db, secret, ns);
            writeMap(out, filters);
            out.writeInt(limit);
            out.writeInt(offset);

            sendRaw(bout.toByteArray());
        } catch (Throwable t) {
            pendingCount.remove(reqId);
            f.completeExceptionally(t);
        }
        return f;
    }

    @Override
    public CompletableFuture<Void> set(String db, String secret, String ns, String key, String json) {
        CompletableFuture<Void> f = new CompletableFuture<>();
        String reqId = UUID.randomUUID().toString();
        pendingSet.put(reqId, f);

        try {
            ByteArrayOutputStream bout = new ByteArrayOutputStream();
            DataOutputStream out = new DataOutputStream(bout);

            writeHeader(out, "SET", reqId, db, secret, ns);
            out.writeUTF(key);
            out.writeUTF(json);

            sendRaw(bout.toByteArray());
        } catch (Throwable t) {
            pendingSet.remove(reqId);
            f.completeExceptionally(t);
        }
        return f;
    }

    @Override
    public CompletableFuture<Void> delete(String db, String secret, String ns, String key) {
        CompletableFuture<Void> f = new CompletableFuture<>();
        String reqId = UUID.randomUUID().toString();
        pendingDelete.put(reqId, f);

        try {
            ByteArrayOutputStream bout = new ByteArrayOutputStream();
            DataOutputStream out = new DataOutputStream(bout);

            writeHeader(out, "DELETE", reqId, db, secret, ns);
            out.writeUTF(key);

            sendRaw(bout.toByteArray());
        } catch (Throwable t) {
            pendingDelete.remove(reqId);
            f.completeExceptionally(t);
        }
        return f;
    }

    @Override
    public void sendRegisterModel(String db, String secret, String ns, String typeName, Map<String, ModelField> fields) {
        try {
            ByteArrayOutputStream bout = new ByteArrayOutputStream();
            DataOutputStream out = new DataOutputStream(bout);

            // Register doesn't strictly need a reqId or response, so we use "NO-OP" or random
            writeHeader(out, "REGISTER", UUID.randomUUID().toString(), db, secret, ns);
            out.writeUTF(typeName);

            out.writeInt(fields.size());
            for (Map.Entry<String, ModelField> entry : fields.entrySet()) {
                out.writeUTF(entry.getKey());
                ModelField mf = entry.getValue();
                out.writeUTF(mf.jsonPath);
                out.writeUTF(mf.sqlType);
            }
            sendRaw(bout.toByteArray());
        } catch (Throwable t) {
            logWarn("Failed to register model: " + t.getMessage());
        }
    }

    // --- Helper Methods ---

    protected void writeHeader(DataOutputStream out, String op, String reqId, String db, String secret, String ns) throws IOException {
        out.writeUTF(op);
        out.writeUTF(reqId);
        out.writeUTF(db);
        out.writeUTF(secret); // Auth Secret
        out.writeUTF(ns);
    }

    protected void writeMap(DataOutputStream out, Map<String, String> map) throws IOException {
        if (map == null) {
            out.writeInt(0);
        } else {
            out.writeInt(map.size());
            for (Map.Entry<String, String> entry : map.entrySet()) {
                out.writeUTF(entry.getKey());
                out.writeUTF(entry.getValue());
            }
        }
    }

    protected void sendRaw(byte[] data) throws IOException {
        if (socket == null || socket.isClosed()) throw new IOException("Socket closed");
        synchronized (out) {
            out.writeInt(data.length); // Frame Length
            out.write(data);           // Payload
            out.flush();
        }
    }

    // --- Network Loop & Response Handling ---

    protected void listen() {
        while (running) {
            try {
                if (socket == null || socket.isClosed()) {
                    Thread.sleep(2000);
                    connect(); // Auto-reconnect
                    continue;
                }

                int len = in.readInt();
                byte[] data = new byte[len];
                in.readFully(data);

                processResponse(data);

            } catch (Exception e) {
                if (running) {
                    logWarn("Link broken (" + e.getMessage() + "), reconnecting in 2s...");
                    try { socket.close(); Thread.sleep(2000); } catch (Exception ignored) {}
                }
            }
        }
    }

    protected void processResponse(byte[] data) {
        try {
            DataInputStream in = new DataInputStream(new ByteArrayInputStream(data));
            String tag = in.readUTF(); // "RES"
            if (!"RES".equals(tag)) return;

            String reqId = in.readUTF();
            boolean ok = in.readBoolean();

            boolean hasVal = in.readBoolean();
            String val = hasVal ? in.readUTF() : null;
            boolean hasErr = in.readBoolean();
            String err = hasErr ? in.readUTF() : null;

            // GET / SEARCH
            CompletableFuture<Optional<String>> fg = pendingGet.remove(reqId);
            if (fg != null) {
                if (ok) fg.complete(Optional.ofNullable(val));
                else fg.completeExceptionally(new IOException(err != null ? err : "Get request failed"));
                return;
            }

            // COUNT
            CompletableFuture<Optional<Integer>> fc = pendingCount.remove(reqId);
            if (fc != null) {
                if (ok && val != null) {
                    try {
                        fc.complete(Optional.of(Integer.parseInt(val)));
                    } catch (Exception e) {
                        fc.completeExceptionally(new IOException("Invalid count response: " + val));
                    }
                } else {
                    if (!ok) fc.completeExceptionally(new IOException(err != null ? err : "Count request failed"));
                    else fc.complete(Optional.empty());
                }
                return;
            }

            // SET
            CompletableFuture<Void> fs = pendingSet.remove(reqId);
            if (fs != null) {
                if (ok) fs.complete(null);
                else fs.completeExceptionally(new IOException(err != null ? err : "Set request failed"));
                return;
            }

            // DELETE
            CompletableFuture<Void> fd = pendingDelete.remove(reqId);
            if (fd != null) {
                if (ok) fd.complete(null);
                else fd.completeExceptionally(new IOException(err != null ? err : "Delete request failed"));
            }

        } catch (Throwable t) {
            logWarn("Bad response format: " + t);
        }
    }

    // --- Logging Stubs  ---

    protected void logInfo(String msg) {
        System.out.println("[SimpleTcp] " + msg);
    }
    protected void logWarn(String msg) {
        System.err.println("[SimpleTcp] WARN: " + msg);
    }
    protected void logSevere(String msg) {
        System.err.println("[SimpleTcp] ERROR: " + msg);
    }
}