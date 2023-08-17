import java.util.ArrayList;
import java.util.List;

public class ConnectionToDB {

    public List<Lobby> getLobbyList() {
        List<Lobby> lobbyList = new ArrayList<>();
        lobbyList.add(new Lobby("Lobby 1", "8.8.8.8", 4, 1, Lobby.Difficulty.MEDIUM, "password"));
        lobbyList.add(new Lobby("Lobby 2", "123.23.213.2", 8, 2, Lobby.Difficulty.HARD, "notPassword"));
        return lobbyList;
    }

}
