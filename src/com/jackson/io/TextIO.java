package com.jackson.io;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class TextIO {

    private TextIO() {
    }


    public static List<String> readFile(String dir) {

        List<String> data = new ArrayList<>();
        try {
            BufferedReader reader = new BufferedReader(new FileReader(dir));

            String readLine = "";
            while(true) {
                readLine = reader.readLine();
                if(readLine == null) {
                    break;
                }
                data.add(readLine);
            }
            reader.close();
        } catch (IOException ignored) {
        }
        return data;
    }

    public static boolean updateFile(List<String> data, String dir) {

        if(data == null || data.isEmpty()) { //Gatekeeping
            return false;
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
            return false;
        }
        return true; // returns true if file write was successful
    }




    public static boolean writeMap(String[][] bitmap, String dir) {
        if(bitmap == null || bitmap.length == 0) { //Gatekeeping
            return false;
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
            return false;
        }
        return true;
    }

    public static String[][] readMapFile() {
        String dir = "resources/multiplayer.txt";

        if(!Files.exists(Path.of(dir))) {
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
