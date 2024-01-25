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
import java.util.*;

public class Server {

    private static final int PORT = 4234;
    private ServerSocket serverSocket;
    private final String SETTINGS_DIRECTORY = "resources/multiplayer_settings.txt";
    private final Set<Integer> zombieIds;
    private final Set<Integer> droppedBlocksIds;

    //Game Fields
    private String[][] map;
    private final List<ClientHandler> players;
    private final GameHandler gameHandler;

    public Server() {
        //List for all connections
        players = new ArrayList<>();
        zombieIds = new HashSet<>();
        droppedBlocksIds = new HashSet<>();
        try {
            //Initialises the server socket with the specified port
            this.serverSocket = new ServerSocket(PORT);
        } catch (IOException e) {
            //Outputs appropriate error message
            System.err.println("Error: Initialising Server Failed");
        }

        map = TextIO.readMapFile();

        gameHandler = new GameHandler();
        gameHandler.start();

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

    private class GameHandler {

        Difficulty difficulty;
        Random rand;
        private final double SPAWN_RATE;

        public GameHandler() {
            if(!TextIO.readFile(SETTINGS_DIRECTORY).isEmpty()) {
                //Send saved world difficulty
                difficulty = Difficulty.valueOf(TextIO.readFile(SETTINGS_DIRECTORY).get(0));
            } else {
                //If nothing saved send easy
                difficulty = Difficulty.EASY;
            }
            rand = new Random();

            SPAWN_RATE = switch (difficulty) {
                case EASY -> 0.001;
                case MEDIUM -> 0.0012;
                case HARD -> 0.0015;
            };
        }



        public void start() {
            Timer timer = new Timer(true);
            timer.scheduleAtFixedRate(new TimerTask() {
                @Override
                public void run() {

//                    try {
//                        spawnZombies();
//                    } catch (IOException e) {
//                        throw new RuntimeException(e);
//                    }

                }
            }, 0, 1000/60);
        }

        private void spawnZombies() throws IOException {
            if(rand.nextDouble() > SPAWN_RATE * players.size()) return; //No spawn
            if(players.isEmpty()) return;

            //Choose unlucky player
            ClientHandler player = players.get(rand.nextInt(players.size()));
            int spawnXPos = player.xPos - 18 + rand.nextInt(32);
            int packSize = (int) rand.nextGaussian(3, 1);
            int[][] packLocation = new int[packSize+1][2];
            packLocation[0][0] = spawnXPos;
            packLocation[0][1] = findSolidBlock(spawnXPos);
            for (int i = 0; i < packSize; i++) {
                //xpos, ypos followed by zombie data
                packLocation[i+1][0] = rand.nextInt(25);
                packLocation[i+1][1] = assignNewID(zombieIds);
            }


            for(ClientHandler handler : players) {
                handler.send("zombie_spawn", player.displayName, packLocation);
            }

        }

        private int findSolidBlock(int xPos) {
            if(xPos < 0) return 0; //Not valid
            for (int i = 0; i < map[xPos].length; i++) {
                if(map[xPos][i].equals("0") || map[xPos][i].equals("6") || (map[xPos][i].equals("5"))) continue;
                return i;
            }
            return 0;
        }

        private int assignNewID(Set<Integer> set) {
            //Finds next available index
            int counter = 0;
            while(true) {
                if(set.contains(counter)) {
                    counter++;
                    continue;
                }
                set.add(counter);
                return counter;
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
                    String MAP_DIRECTORY = "resources/multiplayer.txt";
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
                        data = new int[]{500, findStartingY(), 0, 0}; //Spawn
                    } else {
                        //Saved Location
                        xPos = Integer.parseInt(playerData.get(0));
                        yPos = Integer.parseInt(playerData.get(1));
                        xOffset = Integer.parseInt(playerData.get(2));
                        yOffset = Integer.parseInt(playerData.get(3));
                        data = new int[]{xPos, yPos, xOffset, yOffset};
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

                case "damage_zombie", "update_zombie_pos" -> {
                    for(ClientHandler player : players) {
                        if(player == this) continue;
                        player.send(packet);
                    }
                }

                case "create_dropped_item" -> {
                    int id = gameHandler.assignNewID(droppedBlocksIds);
                    int[] data = (int[]) packet.getObject();
                    data[3] = id; //Add id
                    for(ClientHandler handler : players) {
                        handler.send("create_dropped_item", packet.getExt(), data);
                    }
                }


            }
        }

        private int findStartingY() {
            for (int i = 0; i < map[500].length; i++) {
                if(map[500][i].equals("2")) return i;
            }
            return 0;
        }

        private void send(String msg, Object object) throws IOException {
            outStream.writeObject(new Packet(msg, object)); //Send packet to client
        }

        private void send(String msg, String ext, Object object) throws IOException {
            Packet packet = new Packet(msg, object, ext);
            outStream.writeObject(packet); //Send packet to client
        }

        private void send(Packet packet) throws IOException {
            outStream.writeObject(packet);
        }




        private int[] getPosData() {
            return new int[]{xPos, yPos, xOffset, yOffset + 16};
        }


    }


}
