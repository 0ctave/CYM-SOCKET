package fr.craftyourmind.socket.api;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;

import java.util.*;

public class Message {

    private String server = "none";
    private String channel;
    private UUID id;
    private String request;
    private String format;

    private List<String> header = new ArrayList<>();
    private List<Object> body = new ArrayList<>();

    private List<Object> response = new ArrayList<>();

    private Iterator<Object> iterator;

    public Message(String request, Object... elements) {
        this("CYM", request, elements);
    }

    public Message(String channel, String request, Object[] elements) {
        this.channel = channel;

        id = UUID.randomUUID();
        this.request = request;
        header.add(id.toString());
        header.add(request);
        if (elements.length > 0) {
            List<Object> elementsList = Arrays.asList(elements);
            StringBuilder format = new StringBuilder();
            for (Object element : elementsList)
                if (element instanceof String)
                    format.append("s/");
                else if (element instanceof Integer)
                    format.append("i/");
                else if (element instanceof Float)
                    format.append("f/");
                else if (element instanceof Double)
                    format.append("d/");
                else if (element instanceof Boolean)
                    format.append("b/");
                else if (element instanceof Short)
                    format.append("m/");
                else if (element instanceof Long)
                    format.append("l/");
            this.format = format.toString();
            header.add(this.format);
            body.addAll(Arrays.asList(elements));
        } else {
            this.format = "null";
            header.add(this.format);
        }
    }

    public Message(ByteArrayDataInput input) {
        channel = input.readUTF();

        id = UUID.fromString(input.readUTF());
        request = input.readUTF();
        format = input.readUTF();
        header.add(id.toString());
        header.add(request);
        header.add(format);
        if(!format.equals("null")) {
            String[] formats = format.split("/");
            List<Object> elementsList = new ArrayList<>();
            for (String element : formats) {
                switch (element) {
                    case "s":
                        elementsList.add(input.readUTF());
                        break;
                    case "i":
                        elementsList.add(input.readInt());
                        break;
                    case "f":
                        elementsList.add(input.readFloat());
                        break;
                    case "d":
                        elementsList.add(input.readDouble());
                        break;
                    case "b":
                        elementsList.add(input.readBoolean());
                        break;
                    case "m":
                        elementsList.add(input.readShort());
                        break;
                    case "l":
                        elementsList.add(input.readLong());
                        break;
                }
            }
            body.addAll(elementsList);
        }
    }

    public void addElement(Object element) {
        body.add(element);
    }

    public void addElements(Object... elements) {
        body.addAll(Arrays.asList(elements));
    }

    public void addResponse(Object element) {
        response.add(element);
    }

    public void addResponses(Object... elements) {
        response.addAll(Arrays.asList(elements));
    }

    public byte[] toBytes() {
        ByteArrayDataOutput output = ByteStreams.newDataOutput();

        output.writeUTF(channel);

        for (String opt : header) {
            output.writeUTF(opt);
        }

        arrayToBytes(output, body);

        return output.toByteArray();
    }

    public ByteArrayDataOutput arrayToBytes(ByteArrayDataOutput output, List<Object> array) {
        for (Object element : array) {
            if (element instanceof String)
                output.writeUTF((String) element);
            else if (element instanceof Integer)
                output.writeInt((Integer) element);
            else if (element instanceof Float)
                output.writeFloat((Float) element);
            else if (element instanceof Double)
                output.writeDouble((Double) element);
            else if (element instanceof Boolean)
                output.writeBoolean((Boolean) element);
            else if (element instanceof Short)
                output.writeShort((Short) element);
            else if (element instanceof Long)
                output.writeLong((Long) element);
        }

        return output;
    }


    public byte[] getResponse() {
        ByteArrayDataOutput output = ByteStreams.newDataOutput();

        output.writeUTF(channel);
        output.writeUTF(id.toString());
        output.writeUTF(request);
        output.writeUTF("null");

        arrayToBytes(output, response);

        return output.toByteArray();
    }

    public String getChannel() {
        return channel;
    }

    protected void setChannel(String channel) {
        this.channel = channel;
    }

    public String getRequest() {
        return request;
    }

    public UUID getUUID() {
        return id;
    }

    public String getFormat() {
        return format;
    }

    public List<Object> getBody() {
        return body;
    }

    public Object next() {
        if (iterator == null)
            iterator = body.iterator();
        return iterator.next();
    }

    public void setServer(String serverName) {
        this.server = server;
    }

    public String getServer() {
        return this.server;
    }
}
