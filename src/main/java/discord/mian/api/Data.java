package discord.mian.api;

import java.io.File;
import java.io.IOException;

public interface Data {
    String getName();

    AIDocument getDocument();
    void saveDocument();

    File getPromptFile();

    String getPrompt() throws IOException;

    void addOrReplacePrompt(String text) throws IOException;

    void nuke() throws IOException;
}
