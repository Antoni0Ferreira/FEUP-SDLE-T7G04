package utils;

import java.io.*;
import java.lang.reflect.Array;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

public class Message implements Serializable {

    public enum Type {
        // Authentication
        AUTH,
        AUTH_OK,
        AUTH_FAIL,

        // Server Manager
        UPDATE_TABLE,
        CREATE_LIST,
        LIST_CREATED,
        DELETE_LIST,
        LIST_DELETED,
        PUSH_LIST,
        LIST_PUSHED,
        PULL_LIST,
        LIST_PULLED,
        LIST_NOT_FOUND,
        ADD_SERVER,
        SERVER_NOT_FOUND,
    }

    public enum Sender {
        CLIENT,
        SERVER,
        SERVER_MANAGER
    }

    private final Type type;
    private Sender sender;
    private final Object content;

    private Integer id;

    public Message( Type type, Object content) {
        this.type = type ;
        this.content = content;
    }

    public Type getType() {
        return type;
    }

    public Integer getId() {return id;}

    public void setId(Integer id) {
        this.id = id;
    }
    public Sender getSender() {
        return sender;
    }

    public void setSender(Sender sender) {
        this.sender = sender;
    }

    public Object getContent() {
        return content;
    }

    public boolean sendMessage(SocketChannel channel) throws IOException {

        ByteArrayOutputStream byteOUTStream = new ByteArrayOutputStream();
        ObjectOutputStream objOUTStream = new ObjectOutputStream(byteOUTStream);

        objOUTStream.writeObject(this);

        byte[] numBytes = byteOUTStream.toByteArray();

        int numBytesLength = numBytes.length;

        ByteBuffer buffer = ByteBuffer.allocate(4 + numBytesLength);

        buffer.putInt(numBytesLength);
        buffer.put(numBytes);

        buffer.flip();

        return channel.write(buffer) > 0;
    }

    public static Message readMessage(SocketChannel channel) throws IOException, ClassNotFoundException {
        ByteBuffer lengthBuffer = ByteBuffer.allocate(4);
        while(lengthBuffer.hasRemaining()) {
            if(channel.read(lengthBuffer) == -1) {
                throw new EOFException("End of stream reached before reading the length");
            }
        }
        lengthBuffer.flip();
        int messageLength = lengthBuffer.getInt();

        if (messageLength < 0) {
            throw new IOException("Invalid message length");
        }

        ByteBuffer messageBuffer = ByteBuffer.allocate(messageLength);
        while(messageBuffer.hasRemaining()) {
            if(channel.read(messageBuffer) == -1) {
                throw new EOFException("End of stream reached before reading the full message");
            }
        }
        messageBuffer.flip();
        byte[] messageBytes = new byte[messageLength];
        messageBuffer.get(messageBytes);

        ObjectInputStream objINStream = new ObjectInputStream(new ByteArrayInputStream(messageBytes));
        return (Message) objINStream.readObject();
    }


}
