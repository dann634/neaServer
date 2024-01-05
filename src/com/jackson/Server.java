package com.jackson;

import com.jackson.game.Difficulty;
import com.jackson.io.TextIO;
import com.jackson.network.shared.Packet;

import javax.management.modelmbean.InvalidTargetObjectTypeException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.spec.RSAOtherPrimeInfo;
import java.util.ArrayList;
import java.util.List;

public class Server {
    /*
    All Requests
    ll - Requests lobby list - Return List<com.jackson.network.shared.Lobby> Back from Database
     */

    private static final int PORT = 4234;
    private ServerSocket serverSocket;

    //Game Fields
    private String[][] map;
    private List<ClientHandler> players;

    public Server() {
        players = new ArrayList<>();
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
        private final Socket clientSocket;
        private ObjectOutputStream outStream;
        private ObjectInputStream inStream;
        private int xPos;
        private int yPos;
        private int xOffset;
        private int yOffset;
        private String displayName;

        public ClientHandler(Socket clientSocket) {
            this.clientSocket = clientSocket;
        }

        @Override
        public void run() { //overridden method from Thread
            try {
                this.outStream = new ObjectOutputStream(this.clientSocket.getOutputStream()); //Connects outstream to socket
                this.inStream = new ObjectInputStream(this.clientSocket.getInputStream()); //Connects instream to socket
                map = TextIO.readMapFile();

                while(this.clientSocket.isConnected()) {
                    Packet packet = (Packet) this.inStream.readObject(); //Waits for new incoming object from client
                    processRequest(packet); //Casts from Object to String and processes response
                }

            } catch (Exception e) {
                //Prints error from thread name and closes client to prevent more errors
//                System.err.println("Error on " + getName() + ": closing client...");
                players.remove(this);
            }
        }

        private void processRequest(Packet packet) throws IOException {
            switch (packet.getMsg()) {
                case "map" -> {
                    map = (String[][]) packet.getObject();
                    Files.deleteIfExists(Path.of("resources/multiplayer.txt"));
                    Files.createFile(Path.of("resources/multiplayer.txt"));
                    TextIO.writeMap(map, "resources/multiplayer.txt");

                }
                case "join" -> {
                    //Set values
                    //xPos, yPos, xOffset, yOffset
                    displayName = (String) packet.getObject();
                    //Check for data file
                    send("map", map); //Send map to client


                    String dir = "resources/player_files/" + displayName + ".txt";
                    if(Files.notExists(Path.of(dir))) { // FIXME: 04/01/2024 display name could be an invalid file name
                        Files.createFile(Path.of(dir));
                    }
                    List<String> playerData = TextIO.readFile(dir);
                    send("player_data", playerData);
                    int[] data;
                    if(playerData.isEmpty()) {
                        data = new int[]{500, findStartingY(map), 0, 0};
                    } else {
                        data = new int[]{Integer.parseInt(playerData.get(0)), Integer.parseInt(playerData.get(1)),
                                Integer.parseInt(playerData.get(2)), Integer.parseInt(playerData.get(3))};
                    }

                    if(Files.exists(Path.of("resources/multiplayer_settings.txt")) && !TextIO.readFile("resources/multiplayer_settings.txt").isEmpty()) {
                        Difficulty difficulty = Difficulty.valueOf(TextIO.readFile("resources/multiplayer_settings.txt").get(0));
                        send("difficulty", difficulty);
                    } else {
                        send("difficulty", Difficulty.EASY);
                    }


                    //Get data for everyone else
                    for(ClientHandler player : players) {
                        //Everyone else loads you
                        player.send("player_join", displayName, data);

                        //Load everyone already in game
                        send("player_join", player.displayName, player.getPosData());
                        System.out.println(player.displayName);
                    }
                    players.add(this);
                }

                case "world_check" -> {
                    //Does multiplayer world exist
                    send("world_check_response", Files.exists(Path.of("resources/multiplayer.txt")));
                }

                case "username_check" -> { //to avoid two of the same username joining
                    String displayName = (String) packet.getObject();
                    for(ClientHandler player : players) {
                        if(player != this && player.displayName.equals(displayName)) {
                            send("username_check", false);
                            return;
                        }
                    }
                    send("username_check", true);
                }

                case "save_player_data" -> {
                    ArrayList<String> data = (ArrayList<String>) packet.getObject();
                    String displayName = data.get(data.size()-1);
                    String dir = "resources/player_files/" + displayName + ".txt";
                    if(Files.notExists(Path.of(dir))) {
                        Files.createFile(Path.of(dir));
                    }
                    data.remove(data.size()-1); // TODO: 04/01/2024 maybe remove
                    TextIO.updateFile(data, dir);
                    players.remove(this);
                }

                case "pos_update" -> {
                    int[] data = (int[]) packet.getObject();
                    xPos = data[0];
                    yPos = data[1];
                    xOffset = data[2];
                    yOffset = data[3];
                    for(ClientHandler handler : players) {
                        if(handler != this) {
                            handler.send("pos_update", displayName, data);
                        }
                    }
                }
            }
        }

        private void send(String msg, Object object) throws IOException {
            outStream.writeObject(new Packet(msg, object));
        }

        private void send(String msg, String ext, Object object) throws IOException {
            Packet packet = new Packet(msg, object);
            packet.setExt(ext);
            outStream.writeObject(packet);
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

        private int findStartingY(String[][] map) {
            for (int i = 0; i < 300; i++) {
                if(map[500][i].equals("2")) {
                    return i;
                }
            }
            return -1;
        }

        private int[] getPosData() {
            return new int[]{xPos, yPos, xOffset, yOffset};
        }


    }
}
