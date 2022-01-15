package com.catkatpowered.katserver.network;

import io.javalin.Javalin;

public class KatNetwork {
    public static void KatNetworkMain() {
        Javalin katserver = Javalin.create();
        // HTTP Handlers
        //katserver.get("/http", );

        // WebSocket Handlers
        katserver.ws("/websocket", ws -> ws.onMessage(WebSocketHandler::WebSocketMessageHandler));
    }
}