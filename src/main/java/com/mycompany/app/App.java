package com.mycompany.app;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;

import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;

public class App {

    private static final String MESSAGE = "Hello World!";

    public App() {}

    public static void main(String[] args) throws IOException {
        // Create HTTP server listening on port 8081
        HttpServer server = HttpServer.create(new InetSocketAddress(8081), 0);

        // Handle requests to "/"
        server.createContext("/", new HttpHandler() {
            @Override
            public void handle(HttpExchange exchange) throws IOException {
                byte[] response = MESSAGE.getBytes();
                exchange.sendResponseHeaders(200, response.length);
                OutputStream os = exchange.getResponseBody();
                os.write(response);
                os.close();
            }
        });

        server.setExecutor(null); // default executor
        server.start();
        System.out.println("Server started on port 8081");
    }

    public String getMessage() {
        return MESSAGE;
    }
}
