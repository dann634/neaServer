package com.jackson;

import com.jackson.network.shared.RequestPacket;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class Server {
    /*
    All Requests
    ll - Requests lobby list - Return List<com.jackson.network.shared.Lobby> Back from Database
     */

    private static final int PORT = 4234;
    private ServerSocket serverSocket;
    private List<ClientHandler> clientList;
    private ConnectionToDB connectionToDB;

    public Server() throws IOException {
        this.connectionToDB = new ConnectionToDB();
        this.clientList = new ArrayList<>();
        try {
            this.serverSocket = new ServerSocket(PORT);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        while(true) {
            try {
                new ClientHandler(serverSocket.accept()).start();
                System.out.println("Client connected");
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private class ClientHandler extends Thread {
        private Socket clientSocket;
        private ObjectOutputStream outStream;
        private ObjectInputStream inStream;

        public ClientHandler(Socket clientSocket) {
            this.clientSocket = clientSocket;
        }

        @Override
        public void run() {
            clientList.add(this);
            try {
                this.outStream = new ObjectOutputStream(this.clientSocket.getOutputStream());
                this.inStream = new ObjectInputStream(this.clientSocket.getInputStream());
                while(this.clientSocket.isConnected()) {
                    Object incomingObj = this.inStream.readObject();
                    System.out.println("Request received");
                    processRequest((RequestPacket) incomingObj);
                }
            } catch (IOException e) {
            } catch (ClassNotFoundException e) {
                throw new RuntimeException(e);
            }

        }

        private void processRequest(RequestPacket packet) throws IOException {
            switch (packet.request) {
                case "ll":
                    //get lobby list
                    sendLobbyList();
                    break;
            }
        }

        private void sendLobbyList()  {
            try {
                this.outStream.writeObject(connectionToDB.getLobbyList());
                this.outStream.close();
                this.inStream.close();
            } catch (IOException e) {
                System.err.println("Error: com.jackson.network.shared.Lobby List Failed to Send");
                e.printStackTrace();
            }
        }


    }
}
