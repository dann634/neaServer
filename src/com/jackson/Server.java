package com.jackson;

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
    private ConnectionToDB connectionToDB;

    public Server() {
        this.connectionToDB = new ConnectionToDB(); //Connects to Database
        try {
            //Initialises the server socket with the specified port
            this.serverSocket = new ServerSocket(PORT);
        } catch (IOException e) {
            //Outputs appropriate error message
            System.err.println("Error: Initialising Server Failed");
        }

        while(true) { //Always listening for more clients
            try {
                //Waits for new clients and then makes a new client handler on a new thread
                new ClientHandler(serverSocket.accept()).start();

            } catch (IOException e) {
                //Appropriate Error Message
                System.err.println("Error: Client Connection Failed");
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
        public void run() { //overridden method from Thread
            try {
                this.outStream = new ObjectOutputStream(this.clientSocket.getOutputStream()); //Connects outstream to socket
                this.inStream = new ObjectInputStream(this.clientSocket.getInputStream()); //Connects instream to socket

                Object incomingObj = this.inStream.readObject(); //Waits for new incoming object from client
                processRequest((String) incomingObj); //Casts from Object to String and processes response

            } catch (Exception e) {
                //Prints error from thread name and closes client to prevent more errors
                System.err.println("Error on " + getName() + ": closing client...");
            }
        }

        private void processRequest(String request) {
            switch (request) {
                case "ll": //lobby list request
                    //get lobby list
                    sendLobbyList();
                    break;
                case "ping":
                    sendPing();
                    break;
            }
        }

        private void sendPing() {
            try {
                outStream.writeObject("ping response");
                this.outStream.close(); //closes outstream connection
                this.inStream.close(); //Closes instream connection
            } catch (IOException e) {
                System.err.println("Error: Failed to Ping");
            }
        }

        private void sendLobbyList()  {
            try {
                //Gets List<Lobby> from database
                //Sends on outstream
                this.outStream.writeObject(connectionToDB.getLobbyList());
                this.outStream.close(); //closes outstream connection
                this.inStream.close(); //Closes instream connection
            } catch (IOException e) {
                //Handles error and outputs message
                System.err.println("Error: Lobby List Failed to Send");
            }
        }


    }
}
