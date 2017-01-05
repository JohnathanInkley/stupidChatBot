package com.Tutorial;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Set;

public class SimpleChatbot {
    private static int portNumber;
    private ByteBuffer messageBuffer = ByteBuffer.allocate(1024);
    private Selector channelSelector;
    private int messageCount;

    private SimpleChatbot(int port) {
        portNumber = port;
        messageCount = 0;

        try {
            channelSelector = Selector.open();
            runChatbot();
        } catch (IOException ex) {}
    }

    public static void main(String[] args) {
        SimpleChatbot aBot = new SimpleChatbot(8080);
    }

    private void runChatbot() throws IOException {
        setUpListenerForChannels();
        manageConnections();
    }

    private void setUpListenerForChannels() throws IOException {
        ServerSocketChannel newChanel = createServerSocketChannel();
        SelectionKey newKey = newChanel.register(channelSelector, SelectionKey.OP_ACCEPT);
    }
    private ServerSocketChannel createServerSocketChannel() throws IOException {
        ServerSocketChannel portConnectionChannel = ServerSocketChannel.open();
        portConnectionChannel.configureBlocking(false);
        ServerSocket portSocket = portConnectionChannel.socket();
        InetSocketAddress portAddress = new InetSocketAddress(portNumber);
        portSocket.bind(portAddress);
        System.out.println("Chatbot ready to listen on port " + portNumber);
        return portConnectionChannel;

    }

    private void manageConnections() throws IOException {
        while (true) {
            int selectionNum = channelSelector.select();
            Set keysForConnections = channelSelector.selectedKeys();
            Iterator incomingConnectionsInterator = keysForConnections.iterator();

            while (incomingConnectionsInterator.hasNext()) {
                processChannel(incomingConnectionsInterator);
                incomingConnectionsInterator.remove();
            }
        }
    }

    private void processChannel(Iterator incomingRequest) throws IOException {
        SelectionKey currentRequest = (SelectionKey) (incomingRequest.next());

        if (requestIsNewConnection(currentRequest)) {
            acceptConnection(currentRequest);
        } else if (requestIsNewMessage(currentRequest)) {
            processNewMessage(currentRequest);
        }
    }

    private boolean requestIsNewConnection(SelectionKey currentRequest) {
        return ((currentRequest.readyOps() & SelectionKey.OP_ACCEPT) == SelectionKey.OP_ACCEPT);
    }

    private boolean requestIsNewMessage(SelectionKey currentRequest) {
        return ((currentRequest.readyOps() & SelectionKey.OP_READ) == SelectionKey.OP_READ);
    }

    private void acceptConnection(SelectionKey currentRequest) throws IOException {
        ServerSocketChannel newConnectionChannel = (ServerSocketChannel) currentRequest.channel();
        SocketChannel newChannel = newConnectionChannel.accept();
        newChannel.configureBlocking(false);
        newChannel.register(channelSelector, SelectionKey.OP_READ);
        prepareMessage("\n");
        newChannel.write(messageBuffer);
    }

    private void processNewMessage(SelectionKey messageKey) throws IOException {
        SocketChannel messageChannel = (SocketChannel) messageKey.channel();
        messageBuffer.clear();
        int r = messageChannel.read(messageBuffer);
        String incomingMessage = "";
        messageBuffer.flip();
        while (messageBuffer.hasRemaining()) {
            incomingMessage = incomingMessage + (char) (messageBuffer.get());
        }
        System.out.println(incomingMessage);
        prepareMessage(incomingMessage);
        messageChannel.write(messageBuffer);
        if (messageCount == 3) {
            messageChannel.close();
            messageCount = 0;
        }
    }

    private void prepareMessage(String theirPreviousAnswerRaw) {
        String messageToSend;
        String theirPreviousAnswer = theirPreviousAnswerRaw.substring(0, theirPreviousAnswerRaw.length() - 1);
        switch (messageCount) {
            case 0:
                messageToSend = "Hey there, I'm stupidChatBot! What's your name?\n";
                messageCount++;
                break;
            case 1:
                messageToSend = "Nice to meet you " + theirPreviousAnswer + ". What is your favourite colour?\n";
                messageCount++;
                break;
            case 2:
                if (theirPreviousAnswer.equals("green") || (theirPreviousAnswer.equals("Green"))) {
                    messageToSend = "I'm glad you like green too. Anyway, it was good to talk to you! Bye!\n";
                    messageCount++;
                } else {
                    messageToSend = theirPreviousAnswer + " is a bad colour! I like green! What is your new favourite colour?\n";
                }
                break;
            default:
                messageToSend = "stupidChatBot ERROR blahhhh!";
                break;
        }
        messageBuffer.clear();
        messageBuffer.put(messageToSend.getBytes());
        messageBuffer.flip();
    }

}


