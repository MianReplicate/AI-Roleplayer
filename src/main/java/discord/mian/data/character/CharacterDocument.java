package discord.mian.data.character;

import discord.mian.data.PromptType;
import discord.mian.data.AIDocument;
import org.bson.codecs.pojo.annotations.BsonId;

public class CharacterDocument extends AIDocument {
    @BsonId
    private String name;
    private long server;
    private String avatar;
    private double talkability;
    private String prompt;

    public CharacterDocument() {
        setType(PromptType.CHARACTER.displayName.toLowerCase());
        talkability = 0.5;
    }

    public CharacterDocument(String name, long server){
        super(name, server);
        setType(PromptType.CHARACTER.displayName.toLowerCase());
        talkability = 0.5;
    }

    public CharacterDocument(String name, long server, String avatar, double talkability, String prompt) {
        super(name, server, prompt);
        setType(PromptType.CHARACTER.displayName.toLowerCase());
        this.avatar = avatar;
        this.talkability = talkability;
    }

    public double getTalkability() {
        return talkability;
    }

    public String getAvatar() {
        return avatar;
    }

    public void setAvatar(String avatar) {
        this.avatar = avatar;
    }

    public void setTalkability(double talkability) {
        this.talkability = talkability;
    }
}
