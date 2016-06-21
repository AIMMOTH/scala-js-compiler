package io.cenet.compiler

import org.java_websocket.server.WebSocketServer
import org.java_websocket.handshake.ClientHandshake
import java.net.InetSocketAddress
import org.java_websocket.WebSocket
import java.util.logging.Logger
import java.lang.{Integer => JInteger, Boolean => JBoolean, String => JString}

//class ScalaWebsocket extends WebSocketServer {
  
//  val log = Logger.getLogger(classOf[ScalaWebsocket].getName())
//  
//  override def onOpen(conn: WebSocket , handshake: ClientHandshake ) {
//      log.info("new connection to " + conn.getRemoteSocketAddress());
//  }
//
//  override def onClose(conn: WebSocket , code: JInteger, reason: JString , remote : JBoolean ) {
//      log.info("closed " + conn.getRemoteSocketAddress() + " with exit code " + code + " additional info: " + reason);
//  }
//
//  override def onMessage(conn : WebSocket , message : String ) {
//      log.info("received message from " + conn.getRemoteSocketAddress() + ": " + message);
//  }
//
//  override def onError(conn: WebSocket , ex : Exception ) {
//      log.info("an error occured on connection " + conn.getRemoteSocketAddress()  + ":" + ex);
//  }
//}