package com.jackson.io;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class TextIO {

    private TextIO() {
        //No objects allowed
    }


    public static List<String> readFile(String dir) {
        //Initialise list
        List<String> data = new ArrayList<>();
        try {
            //Initialise Reader (with file directory)
            BufferedReader reader = new BufferedReader(new FileReader(dir));

            String readLine; //Variable that holds the current line of data
            while(true) { //Infinite loop
                readLine = reader.readLine(); //Read line from file
                if(readLine == null) {
                    //break condition
                    break;
                }
                data.add(readLine); //Add line to list
            }
            reader.close(); //Close reader when all data has been read
        } catch (IOException ignored) {
        }
        return data;
    }

    public static void updateFile(List<String> data, String dir) {

        if(data == null || data.isEmpty()) { //Gate keeping
            return;
        }

        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(dir, false));
            writer.write(""); //Clears file
            for(String line : data) {
                writer.write(line + "\n"); //Writes new data and adds line break
            }

            writer.close(); //Closes bufferedWriter
        } catch (IOException e) {
            System.err.println("Error: File Writing Failed");
        }
    }




    public static void writeMap(String[][] bitmap, String dir) {
        if(bitmap == null || bitmap.length == 0) { //Gatekeeping
            return;
        }

        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(dir, false));
            for (int i = 0; i < 300; i++) {
                StringBuilder line = new StringBuilder();
                for (int j = 0; j < 1000; j++) {
                    line.append(bitmap[j][i]);
                }
                writer.write(line.toString());
                writer.write("\n");
            }
            writer.close();
        } catch (IOException e) {
        }
    }

    public static String[][] readMapFile() { // TODO: 08/01/2024 maybe rewrite using readFile()
        String dir = "resources/multiplayer.txt";

        if(Files.notExists(Path.of(dir))) {
            System.err.println("Save file not found");
            return null;
        }

        String[][] mapFile = new String[1000][300];
        try {
            BufferedReader reader = new BufferedReader(new FileReader(dir));
            String[] tempArray;
            for (int i = 0; i < 300; i++) {

                tempArray = reader.readLine().split("");
                if(tempArray.length == 0) {
                    continue;
                }
                for (int j = 0; j < 1000; j++) {
                    mapFile[j][i] = tempArray[j];
                }
            }
            reader.close();

        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return mapFile;
    }

}
