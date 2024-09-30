package discord.mian.loxi.common.ai.prompt;

import discord.mian.loxi.common.api.Type;
import io.github.sashirestela.openai.domain.chat.ChatMessage;

import java.util.List;
import java.util.Map;

public abstract class Prompt<T extends Type> {
    protected Map<T, List<String>> typeMap;
    public abstract ChatMessage.SystemMessage getPrompt();

    public Prompt(Map<T, List<String>> typeMap){
        this.typeMap = typeMap;
    }

    public String toString(){
        return this.getPrompt().getContent();
    }

    public List<String> getType(Type type){
        return this.typeMap.get(type);
    }
}
