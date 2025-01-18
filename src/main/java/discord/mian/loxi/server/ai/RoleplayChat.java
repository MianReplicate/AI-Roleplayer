package discord.mian.loxi.server.ai;

import discord.mian.loxi.server.ai.prompt.*;
import discord.mian.loxi.server.ai.prompt.Character;
import discord.mian.loxi.server.api.BotChat;
import discord.mian.loxi.common.util.Constants;
import io.github.sashirestela.openai.SimpleOpenAI;
import io.github.sashirestela.openai.domain.chat.Chat;
import io.github.sashirestela.openai.domain.chat.ChatMessage;
import io.github.sashirestela.openai.domain.chat.ChatRequest;

import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.stream.Stream;

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

    public void limitHistory(){
//        if(this.history.size() >= maxMessages)
//            history.stream()
//                    .filter(chatMessage -> chatMessage.getRole() != ChatMessage.ChatRole.SYSTEM)
//                    .c;
    }

    public void responseFailed(){
        this.makingResponse = false;
    }

    public void creatingResponse(String username, String content){
        if(makingResponse)
            throw new RuntimeException("Already generating a response!");
        this.limitHistory();
        this.makingResponse = true;

        this.history.add(
                ChatMessage.UserMessage.of(username+": "+content, username)
        );
    }

    public void finishedResponse(String finalResponse){
        this.history.add(
                ChatMessage.AssistantMessage.of(finalResponse, this.character.getType(Character.CharacteristicType.ALIASES).getFirst())
        );
        this.makingResponse = false;
    }

    public ChatRequest createRequest(){
        return ChatRequest.builder()
                .maxTokens(this.maxTokens)
                .model(this.model)
                .messages(history)
                .temperature(1.0)
                .build();
    }

    @Override
    public String sendAndGetResponse(String username, String content) {
        this.creatingResponse(username, content);
        ChatRequest chatRequest = createRequest();
        CompletableFuture<Chat> futureChat = this.openAI.chatCompletions()
                .create(chatRequest);
        Chat chat = futureChat.join();
        String response = chat.firstContent();

        this.finishedResponse(response);
        return response;
    }

    @Override
    public String sendAndStream(String username, String content, Consumer<String> currentResponse){
        this.creatingResponse(username, content);

        ChatRequest chatRequest = createRequest();
        CompletableFuture<Stream<Chat>> futureChat = this.openAI.chatCompletions()
                .createStream(chatRequest);
        Stream<Chat> chat = futureChat.join();
        var fullResponseWrapper = new Object(){String fullResponse = "";};
        chat.filter(chatResp -> chatResp.getChoices().size() > 0 && chatResp.firstContent() != null)
                .map(Chat::firstContent)
                .forEach(partialResponse -> {
                    fullResponseWrapper.fullResponse += partialResponse;
                    currentResponse.accept(fullResponseWrapper.fullResponse);
                });
        this.finishedResponse(fullResponseWrapper.fullResponse);
        return fullResponseWrapper.fullResponse;
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

    @Override
    public void setMaxMessages(int maxMessages){
        this.maxMessages = maxMessages;
    }

    @Override
    public void setMaxTokens(int maxTokens){
        this.maxTokens = maxTokens;
    }

    @Override
    public void setModel(String model){
        this.model = model;
    }

    public static Builder builder(Instruction instruction, Character character){
        return new Builder(instruction, character);
    }

    public static class Builder{
        private final Instruction instruction;
        private final Character character;
        private int maxMessages = 70;
        private int maxTokens = 8192;
        private String model = Constants.LLAMA_MODEL;

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
