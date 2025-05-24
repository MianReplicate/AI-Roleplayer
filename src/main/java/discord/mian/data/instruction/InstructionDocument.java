package discord.mian.data.instruction;

import discord.mian.custom.PromptType;
import discord.mian.data.AIDocument;
import org.bson.codecs.pojo.annotations.BsonId;

public class InstructionDocument extends AIDocument {
    @BsonId
    private String name;
    private long server;
    private String prompt;

    public InstructionDocument(){
        setType(PromptType.INSTRUCTION.displayName.toLowerCase());
    }

    public InstructionDocument(String name, long server){
        super(name, server);
        setType(PromptType.INSTRUCTION.displayName.toLowerCase());
    }

    public InstructionDocument(String name, long server, String prompt){
        super(name, server, prompt);
        setType(PromptType.INSTRUCTION.displayName.toLowerCase());
    }
}
