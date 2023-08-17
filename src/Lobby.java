public class Lobby {
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

    enum Difficulty {
        EASY, MEDIUM, HARD
    }
}
