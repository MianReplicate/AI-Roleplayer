package discord.mian.ai;

public class Model {
    private final String display;
    public final String id;

    public Model(String id, String display) {
        this.display = display;
        this.id = id;
    }

    public String getDisplay() {
        return display != null && !display.isEmpty() ? display : id;
    }

    public String toString() {
        return id + "|" + display;
    }
}
