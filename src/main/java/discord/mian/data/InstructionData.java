package discord.mian.data;

import discord.mian.api.Data;
import discord.mian.api.Chattable;
import io.github.sashirestela.openai.domain.chat.ChatMessage;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

public class InstructionData implements Data, Chattable {
    private final File instructionFile;

    public InstructionData(File instructionFile) {
        this.instructionFile = instructionFile;
    }

    public String getName(){
        return instructionFile.getName().substring(0, instructionFile.getName().lastIndexOf("."));
    }

    public String getPrompt() throws IOException {
        if(!instructionFile.exists())
            return null;

        return Files.readString(instructionFile.toPath(), StandardCharsets.UTF_8);
    }

    public File getPromptFile() {
        if(!instructionFile.exists())
            return null;

        return instructionFile;
    }

    public void addOrReplacePrompt(String text) throws IOException {
        if(!instructionFile.exists())
            instructionFile.createNewFile();

        FileWriter writer = new FileWriter(instructionFile.getPath());
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
                "Instructions and Rules"
        );
    }

    // when worst comes to worst!
    public void nuke(){
        if(instructionFile.exists())
            instructionFile.delete();
    }
}
