package discord.mian.data.instruction;

import discord.mian.api.AIDocument;
import org.bson.codecs.pojo.annotations.BsonId;

public class InstructionDocument implements AIDocument {
    @BsonId
    private String name;
    private String prompt;

    public InstructionDocument(){}

    public InstructionDocument(String name){
        this.name = name;
    }

    public InstructionDocument(String name, String prompt){
        this.name = name;
        this.prompt = prompt;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getPrompt() {
        return prompt;
    }

    @Override
    public void setPrompt(String prompt) {
        this.prompt = prompt;
    }
}
