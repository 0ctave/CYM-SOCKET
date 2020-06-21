package fr.craftyourmind.socket.api.listener;

import fr.craftyourmind.socket.api.Message;
import fr.craftyourmind.socket.api.Socket;
import fr.craftyourmind.socket.api.Tunnel;
import fr.craftyourmind.socket.api.messages.ResponseMessage;

import java.util.function.Consumer;

public class ResponseListener implements Consumer<ResponseMessage> {

    private final Socket socket;

    public ResponseListener(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void accept(ResponseMessage responseMessage) {
        if (responseMessage.getResponseStatus().isOk()) {
            Message message = new Message(responseMessage.getDataInput());
            switch (message.getChannel()) {
                case "CYM":
                    switch (message.getRequest()) {
                        case "handshake":
                            socket.getLogger().info("The Server is connected to the Proxy");
                            break;
                        default:
                    }
                    break;
                default:
                    Tunnel tunnel = socket.getTunnel(message.getChannel());
                    if (tunnel != null) {
                        tunnel.getRequests().remove(message.getUUID());
                        tunnel.getListener().processResponse(message);
                    } else
                        socket.getLogger().debug("A response have been received on the %s Channel, but no Tunnel have been registered on this Channel", message.getChannel());
            }
        } else
            socket.getLogger().severe("An error was caught on the server, it sent a bad response message : " + responseMessage.getResponseStatus());

    }
}
