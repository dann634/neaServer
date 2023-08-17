package com.jackson.network.shared;

import com.jackson.game.Difficulty;

import java.io.Serializable;

public class Lobby implements Serializable {

    private static final long serialVersionUID = 1L;
    private String name;
    private String hostIP;
    private int maxPlayers;
    private int currentPlayers;
    Difficulty difficulty;
    private String password;

    public Lobby(String name, String hostIP, int maxPlayers, int currentPlayers, Difficulty difficulty, String password) {
        this.name = name;
        this.hostIP = hostIP;
        this.maxPlayers = maxPlayers;
        this.currentPlayers = currentPlayers;
        this.difficulty = difficulty;
        this.password = password;
    }


}
