package discord.mian.server.ai;

import com.knuddels.jtokkit.Encodings;
import com.knuddels.jtokkit.api.Encoding;
import com.knuddels.jtokkit.api.EncodingRegistry;
import com.knuddels.jtokkit.api.EncodingType;
import discord.mian.server.ai.prompt.Character;
import discord.mian.server.ai.prompt.Instruction;
import discord.mian.common.util.Constants;
import io.github.sashirestela.openai.SimpleOpenAI;
import io.github.sashirestela.openai.domain.chat.Chat;
import io.github.sashirestela.openai.domain.chat.ChatMessage;
import io.github.sashirestela.openai.domain.chat.ChatRequest;

import java.util.ArrayList;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.stream.Stream;

public class RoleplayChat {
    private SimpleOpenAI llm;
    private boolean makingResponse;
    private int maxMessages;
    private int maxTokens;
    private String model;
    private ArrayList<ChatMessage> history;
    private final EncodingRegistry registry;
    private final Instruction instructions;
    private final Character character;
//    private final WorldLore worldLore;

    private RoleplayChat(Builder builder){
        if(Constants.KEY == null || Constants.KEY.isEmpty())
            throw new RuntimeException("No OpenRouter key set!");
        this.llm = SimpleOpenAI.builder()
                .apiKey(Constants.KEY)
                .baseUrl(Constants.BASE_URL)
                .build();
        this.instructions = builder.instruction;
        this.character = builder.character;
        this.maxTokens = builder.maxTokens;
        this.model = builder.model;
        this.registry = Encodings.newDefaultEncodingRegistry();
        this.restartHistory();
//        this.worldLore = worldLore;
    }

    public void limitHistory(){
        Encoding enc = registry.getEncodingForModel(this.model.substring(this.model.indexOf("/")))
                .orElse(registry.getEncoding(EncodingType.CL100K_BASE)); // Example for OpenAI models

        int tokens = maxTokens;
        while(tokens >= maxTokens){
            StringBuilder combinedText = new StringBuilder();
            for(ChatMessage msg : this.history){
                if(msg instanceof ChatMessage.UserMessage message){
                    combinedText.append(message.getContent());
                } else if(msg instanceof ChatMessage.AssistantMessage message){
                    combinedText.append(message.getContent());
                } else if(msg instanceof ChatMessage.SystemMessage message){
                    combinedText.append(message.getContent());
                }
            };

            tokens = enc.countTokens(combinedText.toString());
            if(tokens >= maxTokens)
                history.remove(2); // the msg right after system and character
        }
        Constants.LOGGER.info(String.valueOf(tokens));
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
                ChatMessage.AssistantMessage.of(finalResponse)
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

    public String createResponse(String content){
        ChatRequest chatRequest = createRequest();
        CompletableFuture<Chat> futureChat = this.llm.chatCompletions()
                .create(chatRequest);
        Chat chat = futureChat.join();

        return chat.firstContent();
    }

    public String sendAndGetResponse(String username, String content) {
        this.creatingResponse(username, content);
        ChatRequest chatRequest = createRequest();
        CompletableFuture<Chat> futureChat = this.llm.chatCompletions()
                .create(chatRequest);
        Chat chat = futureChat.join();
        String response = chat.firstContent();

        this.finishedResponse(response);
        return response;
    }

    public String sendAndStream(String username, String content, Consumer<String> currentResponse){
        this.creatingResponse(username, content);
        ChatRequest chatRequest = createRequest();
        CompletableFuture<Stream<Chat>> futureChat = this.llm.chatCompletions()
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

    public boolean isMakingResponse() {
        return this.makingResponse;
    }

    public ArrayList<ChatMessage> getHistory() {
        return this.history;
    }

    public void restartHistory(){
        history = new ArrayList<>();
        history.add(instructions.getPrompt());
        history.add(character.getPrompt());
//        history.add(worldLore.getPrompt());
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

    public String getModel(){
        return model;
    }

    public Character getCharacter(){
        return character;
    }

    public Instruction getInstructions(){
        return instructions;
    }

    public static Builder builder(Instruction instruction, Character character){
        return new Builder(instruction, character);
    }

    public static class Builder{
        private final Instruction instruction;
        private final Character character;
        private int maxTokens = 8192;
        private String model = Constants.DEFAULT_MODEL;

        private Builder(Instruction instruction, Character character){
            this.instruction = instruction;
            this.character = character;
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
