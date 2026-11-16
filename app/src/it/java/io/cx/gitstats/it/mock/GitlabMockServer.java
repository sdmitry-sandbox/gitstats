package io.cx.gitstats.it.mock;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ConcurrentHashMap;

public class GitlabMockServer {

    private HttpServer server;
    private final ConcurrentHashMap<Long, String> projectResponses = new ConcurrentHashMap<>();

    public void start() throws IOException {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/api/v4/projects/", new HttpHandler() {
            @Override
            public void handle(HttpExchange exchange) throws IOException {
                String path = exchange.getRequestURI().getPath();
                String idStr = path.substring(path.lastIndexOf('/') + 1);
                try {
                    Long id = Long.parseLong(idStr);
                    String json = projectResponses.get(id);
                    if (json != null) {
                        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
                        exchange.getResponseHeaders().set("Content-Type", "application/json");
                        exchange.sendResponseHeaders(200, bytes.length);
                        try (OutputStream os = exchange.getResponseBody()) {
                            os.write(bytes);
                        }
                    } else {
                        exchange.sendResponseHeaders(404, -1);
                    }
                } catch (NumberFormatException e) {
                    exchange.sendResponseHeaders(400, -1);
                }
            }
        });
        server.setExecutor(null);
        server.start();
    }

    public int getPort() {
        return server.getAddress().getPort();
    }

    public String getBaseUrl() {
        return "http://localhost:" + getPort();
    }

    public void stubProject(Long id, String name, String webUrl) {
        String json = String.format("{\"id\":%d,\"name\":\"%s\",\"web_url\":\"%s\"}", id, name, webUrl);
        projectResponses.put(id, json);
    }

    public void stop() {
        if (server != null) {
            server.stop(0);
        }
    }
}
