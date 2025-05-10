package discord.mian.ai.data;

import discord.mian.api.Data;
import discord.mian.api.Promptable;
import io.github.sashirestela.openai.domain.chat.ChatMessage;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

public class InstructionData implements Data, Promptable {
    private final File instructionFile;

    public InstructionData(File instructionFile) {
        this.instructionFile = instructionFile;
    }

    public String getName(){
        return instructionFile.getName();
    }

    public String getDefinition() throws IOException {
        if(!instructionFile.exists())
            return null;

        return Files.readString(instructionFile.toPath(), StandardCharsets.UTF_8);
    }

    public ChatMessage getPrompt(){
        String definition;
        try {
            definition = getDefinition();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

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
