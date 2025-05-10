package discord.mian.api;

import java.io.IOException;

public interface Data {
    String getName();
    String getDefinition() throws IOException;
    void nuke();
}
