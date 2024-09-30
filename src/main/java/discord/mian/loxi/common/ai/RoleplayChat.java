package discord.mian.loxi.common.ai;

import discord.mian.loxi.common.ai.prompt.*;
import discord.mian.loxi.common.ai.prompt.Character;
import discord.mian.loxi.common.api.BotChat;
import discord.mian.loxi.common.util.Constants;
import io.github.sashirestela.openai.SimpleOpenAI;
import io.github.sashirestela.openai.domain.chat.Chat;
import io.github.sashirestela.openai.domain.chat.ChatMessage;
import io.github.sashirestela.openai.domain.chat.ChatRequest;

import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;

public class RoleplayChat implements BotChat {
    private SimpleOpenAI openAI;
    private boolean makingResponse;
    private int maxMessages;
    private int maxTokens;
    private String model;
    private ArrayList<ChatMessage> history;
    private final Instruction instructions;
    private final Character character;
//    private final WorldLore worldLore;

    private RoleplayChat(Builder builder){
        this.openAI = SimpleOpenAI.builder().apiKey(Constants.GROQ_TOKEN).baseUrl(Constants.GROQ_BASE_API).build();
        this.instructions = builder.instruction;
        this.character = builder.character;
        this.maxMessages = builder.maxMessages;
        this.maxTokens = builder.maxTokens;
        this.model = builder.model;
        this.restartHistory();
//        this.worldLore = worldLore;
    }

    @Override
    public String sendAndGetResponse(String username, String content) {
        this.makingResponse = true;

        this.history.add(
                ChatMessage.UserMessage.of(content, username)
        );
        ChatRequest chatRequest = ChatRequest.builder()
                .maxTokens(this.maxTokens)
                .model(this.model)
                .messages(history)
                .temperature(1.0)
                .build();
        CompletableFuture<Chat> futureChat = this.openAI.chatCompletions()
                .create(chatRequest);
        Chat chat = futureChat.join();
        String response = chat.firstContent();

        this.history.add(
                ChatMessage.AssistantMessage.of(response, this.character.getType(Character.CharacteristicType.ALIASES).getFirst())
        );
        this.makingResponse = false;
        return response;
    }

    @Override
    public boolean isMakingResponse() {
        return this.makingResponse;
    }

    @Override
    public ArrayList<ChatMessage> getHistory() {
        return this.history;
    }

    @Override
    public void restartHistory(){
        history = new ArrayList<>();
        history.add(instructions.getPrompt());
        history.add(character.getPrompt());
//        history.add(worldLore.getPrompt());
    }

    public static Builder builder(Instruction instruction, Character character){
        return new Builder(instruction, character);
    }

    public static class Builder{
        private final Instruction instruction;
        private final Character character;
        private int maxMessages = 70;
        private int maxTokens = 8192;
        private String model = "llama3-70b-8192";

        private Builder(Instruction instruction, Character character){
            this.instruction = instruction;
            this.character = character;
        }

        public void setMaxMessages(int maxMessages){
            this.maxMessages = maxMessages;
        }

        public void setMaxTokens(int maxTokens){
            this.maxTokens = maxTokens;
        }

        public void setModel(String model){
            this.model = model;
        }

        public RoleplayChat build(){
            return new RoleplayChat(this);
        }
    }
}
