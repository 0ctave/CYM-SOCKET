package fr.craftyourmind.socket.api.listener;

import fr.craftyourmind.socket.api.Message;

public class DummyListener implements Listener {

    @Override
    public Message processMessage(Message message) {
        return message;
    }

    @Override
    public void processResponse(Message message) {

    }
}
