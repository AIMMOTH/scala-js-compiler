package io.cenet.compiler;

import java.net.InetSocketAddress;
import java.util.logging.Logger;

import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

public class SimpleServer extends WebSocketServer {

  private static Logger log = Logger.getLogger(SimpleServer.class.getName());
  
    public SimpleServer() {
        super(new InetSocketAddress("localhost", 8888));
    }

    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {
      System.out.println("open");
      log.info("new connection to " + conn.getRemoteSocketAddress());
    }

    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {
      System.out.println("close");
      log.info("closed " + conn.getRemoteSocketAddress() + " with exit code " + code + " additional info: " + reason);
    }

    @Override
    public void onMessage(WebSocket conn, String message) {
      System.out.println("message:" + message);
      log.info("received message from " + conn.getRemoteSocketAddress() + ": " + message);
    }

    @Override
    public void onError(WebSocket conn, Exception ex) {
      System.out.println("error:" + ex.getMessage());
      log.warning("an error occured on connection " + (conn != null ? conn.getRemoteSocketAddress() : "null") + ":" + ex);
    }

//    public static void main(String[] args) {
//        String host = "localhost";
//        int port = 8887;
//
//        WebSocketServer server = new SimpleServer();
//        server.run();
//    }
}