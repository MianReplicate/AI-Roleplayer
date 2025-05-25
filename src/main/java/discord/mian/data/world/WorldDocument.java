package discord.mian.data.world;

import discord.mian.data.AIDocument;
import discord.mian.data.PromptType;

public class WorldDocument extends AIDocument {
    private String name;
    private long server;
    private String prompt;

    public WorldDocument() {
        setType(PromptType.WORLD.displayName.toLowerCase());
    }

    public WorldDocument(String name, long server) {
        super(name, server);
        setType(PromptType.WORLD.displayName.toLowerCase());
    }

    public WorldDocument(String name, long server, String prompt) {
        super(name, server, prompt);
        setType(PromptType.WORLD.displayName.toLowerCase());
    }
}
