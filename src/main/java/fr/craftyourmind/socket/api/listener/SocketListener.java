package fr.craftyourmind.socket.api.listener;

import fr.craftyourmind.socket.api.Message;
import fr.craftyourmind.socket.api.Socket;
import fr.craftyourmind.socket.api.Tunnel;
import fr.craftyourmind.socket.api.messages.ReceivedMessage;

import java.util.function.Consumer;

public class SocketListener implements Consumer<ReceivedMessage> {

    private Socket socket;

    public SocketListener(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void accept(ReceivedMessage receivedMessage) {
        Message message = new Message(receivedMessage.getDataInput());
        switch (message.getChannel()) {
            case "CYM":
                switch (message.getRequest()) {
                    case "handshake":
                        if (receivedMessage.canRespond())
                            receivedMessage.respond(message.getResponse());
                        break;
                    default:
                }
            default:
                Tunnel tunnel = socket.getTunnel(receivedMessage.getChannelName());
                if (tunnel != null) {
                    Message response = tunnel.getListener().processMessage(message);
                    if (receivedMessage.canRespond())
                        receivedMessage.respond(response.getResponse());
                } else
                    socket.getLogger().debug("A message have been received on the %s Channel, but no Tunnel have been registered on this Channel", receivedMessage.getChannelName());
        }
    }
}
