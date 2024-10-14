package discord.mian.loxi.server.ai.prompt;

import discord.mian.loxi.server.api.Type;
import io.github.sashirestela.openai.domain.chat.ChatMessage;

import java.util.List;
import java.util.Map;

public class Prompt<T extends Type> {
    protected Map<T, List<String>> typeMap;
    protected String promptTypeName;
    public Prompt(String promptTypeName, Map<T, List<String>> typeMap){
        this.typeMap = typeMap;
        this.promptTypeName = promptTypeName;
    }

    public ChatMessage.SystemMessage getPrompt() {
        StringBuilder content = new StringBuilder();
        for (T promptType : typeMap.keySet()) {
            String wholeString;
            if (promptType == PromptType.DEFAULT) {
                wholeString = "";
                for (String string : typeMap.get(promptType)) {
                    wholeString += string + "\n";
                }
                content.append(wholeString).append("\n");
            } else {
                throw new RuntimeException("Unknown Prompt Type: " + promptType);
            }
        }

        return ChatMessage.SystemMessage.of(
                content.toString(),
                promptTypeName
        );
    }

    public String toString(){
        return this.getPrompt().getContent();
    }

    public List<String> getType(Type type){
        return this.typeMap.get(type);
    }

    public enum PromptType implements Type{
        DEFAULT("Default");
        PromptType(String name){
            this.name = name;
        }

        private final String name;

        @Override
        public String getName() {
            return this.name;
        }
    }
}
