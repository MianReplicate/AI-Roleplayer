package discord.mian.data;

import discord.mian.api.Chattable;
import discord.mian.api.Data;
import io.github.sashirestela.openai.domain.chat.ChatMessage;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

public class WorldData implements Data, Chattable {
    private final File worldFile;

    public WorldData(File worldFile) {
        this.worldFile = worldFile;
    }

    public String getName(){
        return worldFile.getName().substring(0, worldFile.getName().lastIndexOf("."));
    }

    public String getPrompt() throws IOException {
        if(!worldFile.exists())
            return null;

        return Files.readString(worldFile.toPath(), StandardCharsets.UTF_8);
    }

    public File getPromptFile() {
        if(!worldFile.exists())
            return null;

        return worldFile;
    }

    public void addOrReplacePrompt(String text) throws IOException {
        if(!worldFile.exists())
            worldFile.createNewFile();

        FileWriter writer = new FileWriter(worldFile.getPath());
        writer.write(text);
        writer.close();
    }

    public ChatMessage getChatMessage(CharacterData data){
        String definition;
        try {
            definition = getPrompt();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        definition = definition.replaceAll("\\{\\{char}}", data.getName());

        return ChatMessage.SystemMessage.of(
                definition,
                "World Lore"
        );
    }

    // when worst comes to worst!
    public void nuke(){
        if(worldFile.exists())
            worldFile.delete();
    }
}
