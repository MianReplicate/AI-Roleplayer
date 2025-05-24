package discord.mian.data.world;

import discord.mian.api.AIDocument;
import org.bson.codecs.pojo.annotations.BsonId;

public class WorldDocument implements AIDocument {
    @BsonId
    private String name;
    private String prompt;

    public WorldDocument(){}

    public WorldDocument(String name){
        this.name = name;
    }

    public WorldDocument(String name, String prompt){
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
