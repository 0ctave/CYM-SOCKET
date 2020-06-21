package fr.craftyourmind.socket.api.listener;

import fr.craftyourmind.socket.api.Message;

public interface Listener {

    public Message processMessage(Message message);

    public void processResponse(Message message);

}
