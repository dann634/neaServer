package com.jackson;

import com.jackson.game.Difficulty;
import com.jackson.network.shared.Lobby;

import java.util.ArrayList;
import java.util.List;

public class ConnectionToDB {

    public List<Lobby> getLobbyList() {
        List<Lobby> lobbyList = new ArrayList<>();
        lobbyList.add(new Lobby("com.jackson.network.shared.Lobby 1", "8.8.8.8", 4, 1, Difficulty.MEDIUM, "password"));
        lobbyList.add(new Lobby("com.jackson.network.shared.Lobby 2", "123.23.213.2", 8, 2, Difficulty.HARD, "notPassword"));
        return lobbyList;
    }

}
