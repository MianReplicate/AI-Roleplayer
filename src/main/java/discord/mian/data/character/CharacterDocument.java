package discord.mian.data.character;

import discord.mian.api.AIDocument;
import org.bson.codecs.pojo.annotations.BsonId;

public class CharacterDocument implements AIDocument {
    @BsonId
    private String name;
    private String avatar;
    private double talkability;
    private String definition;

    public CharacterDocument() {
        talkability = 0.5;
    }

    public CharacterDocument(String name){
        this();
        this.name = name;
    }

    public CharacterDocument(String name, String avatar, double talkability, String definition) {
        this.name = name;
        this.avatar = avatar;
        this.talkability = talkability;
        this.definition = definition;
    }

    @Override
    public String getName() {
        return name;
    }

    public double getTalkability() {
        return talkability;
    }

    public String getAvatar() {
        return avatar;
    }

    public String getPrompt() {
        return definition;
    }

    public void setAvatar(String avatar) {
        this.avatar = avatar;
    }

    public void setPrompt(String definition) {
        this.definition = definition;
    }

    public void setTalkability(double talkability) {
        this.talkability = talkability;
    }
}
