package com.jackson;

import com.jackson.game.Difficulty;
import com.jackson.io.TextIO;
import com.jackson.network.shared.Packet;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class Server {
    /*
    All Requests
    ll - Requests lobby list - Return List<com.jackson.network.shared.Lobby> Back from Database
     */

    private static final int PORT = 4234;
    private ServerSocket serverSocket;
    private final String SETTINGS_DIRECTORY = "resources/multiplayer_settings.txt";
    private final String MAP_DIRECTORY = "resources/multiplayer.txt";

    //Game Fields
    private String[][] map;
    private final List<ClientHandler> players;

    public Server() {
        //List for all connections
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

        //Client Connection
        private final Socket clientSocket;

        //Output Stream
        private ObjectOutputStream outStream;

        //Input Stream
        private ObjectInputStream inStream;

        //Tracks players location
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
                map = TextIO.readMapFile(); //Text file is saved to memory

                while(this.clientSocket.isConnected()) { //Will only run if the socket is connected
                    Packet packet = (Packet) this.inStream.readObject(); //Waits for new incoming object from client
                    processRequest(packet); //Passes packet to method responsible for managing the requests
                }

            } catch (Exception e) {
                //Removes client from client list
                players.remove(this);
            }
        }

        /*
        map - client is sending a map file to be saved in the multiplayer.txt file
        join - client is requesting to join the server
        world_check - client checking if a world already exists
        username_check - client checking if their username is already in use
        save_player_data - client requesting their player data be saved into a text file
        pos_update - client is sending an update for their in-game position which is then sent to all other connected clients
         */

        private void processRequest(Packet packet) throws IOException {
            switch (packet.getMsg()) {
                case "map" -> {
                    map = (String[][]) packet.getObject();
                    Files.deleteIfExists(Path.of(MAP_DIRECTORY)); //Delete the existing File
                    Files.createFile(Path.of(MAP_DIRECTORY)); //Create a new file
                    TextIO.writeMap(map, MAP_DIRECTORY); //Write the map data to the file
                }

                case "join" -> {
                    //Get display name from join request
                    displayName = (String) packet.getObject();
                    //Send map file
                    send("map", map); //Send map to client

                    String dir = "resources/player_files/" + displayName + ".txt";
                    if(Files.notExists(Path.of(dir))) {
                        //If player save file doens't exist, create one
                        Files.createFile(Path.of(dir));
                    }
                    //Read file even if empty
                    List<String> playerData = TextIO.readFile(dir);
                    send("player_data", playerData); //Send player save data to client

                    //Position Data
                    int[] data;
                    if(playerData.isEmpty()) {
                        data = new int[]{500, findStartingY(map), 0, -60}; //Spawn
                    } else {
                        //Saved Location
                        data = new int[]{Integer.parseInt(playerData.get(0)), Integer.parseInt(playerData.get(1)),
                                Integer.parseInt(playerData.get(2)), Integer.parseInt(playerData.get(3))};
                    }

                    if(!TextIO.readFile(SETTINGS_DIRECTORY).isEmpty()) {
                        //Send saved world difficulty
                        Difficulty difficulty = Difficulty.valueOf(TextIO.readFile(SETTINGS_DIRECTORY).get(0));
                        send("difficulty", difficulty);
                    } else {
                        //If nothing saved send easy
                        send("difficulty", Difficulty.EASY);
                    }

                    //Get data for everyone else
                    for(ClientHandler player : players) {
                        //Everyone else loads you
                        player.send("player_join", displayName, data);

                        //Load everyone already in game
                        send("player_join", player.displayName, player.getPosData());
                    }
                    //Add client to list of clients
                    players.add(this);
                }

                case "world_check" -> {
                    //Does multiplayer world exist
                    send("world_check_response", Files.exists(Path.of("resources/multiplayer.txt")));
                }

                case "username_check" -> { //to avoid two of the same username joining
                    String displayName = (String) packet.getObject(); //Get display name from packet
                    for(ClientHandler player : players) { //Loop through all players
                        if(player != this && player.displayName.equals(displayName)) {
                            //if player has same name and isn't you
                            send("username_check", false);
                            return;
                        }
                    }
                    //There's no one with the same display name
                    send("username_check", true);
                }

                case "save_player_data" -> {
                    List<String> data = (ArrayList<String>) packet.getObject(); //Get data
                    String displayName = packet.getExt(); //Get display name
                    String dir = "resources/player_files/" + displayName + ".txt";
                    if(Files.notExists(Path.of(dir))) {
                        //Create file if it doesn't exist already
                        Files.createFile(Path.of(dir));
                    }
                    //Update file with new data
                    TextIO.updateFile(data, dir);
                    //Remove client from list of players
                    players.remove(this);
                }

                case "pos_update" -> {
                    //get change in x and y and current location in map
                    int[] data = (int[]) packet.getObject();
                    xPos = data[0];
                    yPos = data[1];
                    xOffset = data[2];
                    yOffset = data[3];
                    for(ClientHandler handler : players) {
                        if(handler != this) {
                            //Send this information to everyone else
                            handler.send("pos_update", displayName, data);
                        }
                    }
                }

                case "disconnect" -> {
                    String displayName = (String) packet.getObject();
                    players.remove(this);
                    for(ClientHandler handler : players) {
                        //Send disconnect packet to other players
                        handler.send("disconnect", this.displayName);
                    }
                    interrupt();
                }

                case "remove_block" -> {
                    int[] blockPos = (int[]) packet.getObject();
                    if(blockPos.length < 2) return; //Not valid data
                    map[blockPos[0]][blockPos[1]] = "0"; //Update Map
                    for(ClientHandler player : players) {
                        if(player == this) continue;
                        player.send("remove_block", blockPos);
                    }
                }

                case "place_block" -> {
                    int[] blockPos = (int[]) packet.getObject();
                    if(blockPos.length < 2) return; //not valid data
                    map[blockPos[0]][blockPos[1]] = packet.getExt(); //Update map
                    for(ClientHandler player : players) {
                        if(player == this) continue;
                        player.send("place_block", packet.getExt(), packet.getObject());
                    }
                }
            }
        }

        private void send(String msg, Object object) throws IOException {
            outStream.writeObject(new Packet(msg, object)); //Send packet to client
        }

        private void send(String msg, String ext, Object object) throws IOException {
            outStream.writeObject(new Packet(msg, object, ext)); //Send packet to client
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
            return new int[]{xPos, yPos, xOffset, yOffset-32};
        }


    }
}
