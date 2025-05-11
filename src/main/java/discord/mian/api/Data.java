package discord.mian.api;

import java.io.IOException;

public interface Data {
    String getName();
    String getPrompt() throws IOException;
    void addOrReplacePrompt(String text) throws IOException;
    void nuke() throws IOException;
}
