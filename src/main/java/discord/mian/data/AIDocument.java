package discord.mian.data;

import org.bson.codecs.pojo.annotations.BsonId;

public class AIDocument {
    @BsonId
    private String name;
    private String type;
    private long server;
    private String prompt;

    public AIDocument(){}

    public AIDocument(String name, long server){
        this();
        this.name = name;
        this.server = server;
    }

    public AIDocument(String name, long server, String prompt){
        this(name, server);
        this.prompt = prompt;
    }

    public void setServer(long server) {
        this.server = server;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setPrompt(String prompt) {
        this.prompt = prompt;
    }

    public String getPrompt() {
        return prompt;
    }

    public long getServer(){
        return server;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getType(){
        return type;
    }
}
