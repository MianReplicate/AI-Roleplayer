package discord.mian.api;

public interface AIDocument {
    String getName();
    String getPrompt();
    void setPrompt(String prompt);
    void nuke();
}
