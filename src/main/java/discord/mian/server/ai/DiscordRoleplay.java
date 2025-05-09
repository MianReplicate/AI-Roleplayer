package discord.mian.server.ai;

import com.knuddels.jtokkit.Encodings;
import com.knuddels.jtokkit.api.Encoding;
import com.knuddels.jtokkit.api.EncodingRegistry;
import com.knuddels.jtokkit.api.EncodingType;
import discord.mian.common.AIBot;
import discord.mian.common.util.Util;
import discord.mian.server.ai.prompt.Character;
import discord.mian.server.ai.prompt.Instruction;
import discord.mian.common.util.Constants;
import io.github.sashirestela.openai.SimpleOpenAI;
import io.github.sashirestela.openai.domain.chat.Chat;
import io.github.sashirestela.openai.domain.chat.ChatMessage;
import io.github.sashirestela.openai.domain.chat.ChatRequest;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.requests.restaction.MessageCreateAction;
import net.dv8tion.jda.api.utils.TimeUtil;
import net.dv8tion.jda.api.utils.cache.SortedSnowflakeCacheView;
import net.dv8tion.jda.api.utils.messages.MessageCreateData;
import net.dv8tion.jda.api.utils.messages.MessageEditData;

import java.time.OffsetDateTime;
import java.time.temporal.ChronoField;
import java.time.temporal.TemporalField;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.stream.Stream;

public class DiscordRoleplay {
    // limit roleplay to one channel lmfao

    private SimpleOpenAI llm;
    private boolean makingResponse;
    private int maxTokens;
    private String model;
    private final Guild guild;
    private TextChannel channel;
    // need to create a new arraylist that stores chats
    private ArrayList<ChatMessage> preHistory;
    private ArrayList<Long> history;
    private final EncodingRegistry registry;
    private final Instruction instructions;
    private final Character character;
//    private String funnyMessage;
//    private final WorldLore worldLore;

    private DiscordRoleplay(Builder builder){
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
        this.guild = builder.guild;
        this.channel = guild.getChannelById(TextChannel.class, 1330081065393586258L); // test
        this.restartHistory();
//        this.worldLore = worldLore;
    }

    public void trimHistoryIfNeeded(){
        Encoding enc = registry.getEncodingForModel(this.model.substring(this.model.indexOf("/")))
                .orElse(registry.getEncoding(EncodingType.CL100K_BASE));

        int tokens = maxTokens;
        while(tokens >= maxTokens){
            StringBuilder combinedText = new StringBuilder();
            for(ChatMessage msg : getHistoryConverted()){
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
    }

    public void responseFailed(){
        this.makingResponse = false;
    }

    public void creatingResponseFromDiscordMessage(Message msg){
        if(makingResponse)
            throw new RuntimeException("Already generating a response!");
        this.trimHistoryIfNeeded();
        this.makingResponse = true;

        this.history.add(msg.getIdLong());
//        this.history.add(
//                ChatMessage.UserMessage.of(username+": "+content, username)
//        );
    }

    public void finishedDiscordResponse(Message message){
//        this.history.add(
//                ChatMessage.AssistantMessage.of(message.getContentRaw())
//        );
        this.history.add(message.getIdLong());
        this.makingResponse = false;
    }

    public ChatRequest createRequest(){
        return ChatRequest.builder()
                .maxCompletionTokens(this.maxTokens)
                .model(this.model)
                .messages(getHistoryConverted())
                .temperature(1.0)
                .build();
    }

    public String createCustomResponse(String content){
        ArrayList<ChatMessage> history = new ArrayList<>(this.preHistory);
        history.add(ChatMessage.UserMessage.of(content, "Admin"));

        ChatRequest chatRequest = ChatRequest.builder()
                .maxCompletionTokens(this.maxTokens)
                .model(this.model)
                .messages(history)
                .build();

        CompletableFuture<Chat> futureChat = this.llm.chatCompletions()
                .create(chatRequest);
        Chat chat = futureChat.join();

        return chat.firstContent();
    }

    public void sendDiscordMsgAndStream(Message userMsg) {
        if (isMakingResponse()) {
            userMsg.getChannel().sendMessage(MessageCreateData.fromContent(
                    Util.botifyMessage("Cannot make a response since I am already generating one!")
            )).queue();
            return;
        }

        this.creatingResponseFromDiscordMessage(userMsg);

        MessageCreateAction messageCreateData = userMsg.getChannel().sendMessage(
                MessageCreateData.fromContent(
                        Util.botifyMessage("Currently creating a response! Check back in a second..")
                )
        );
        messageCreateData.addActionRow(
                Button.primary("back", "<--"),
                Button.primary("next", "-->")
        );

        messageCreateData.queue(aiMsg -> {
            try {
                ChatRequest chatRequest = createRequest();
                CompletableFuture<Stream<Chat>> futureChat = this.llm.chatCompletions()
                        .createStream(chatRequest);
                Stream<Chat> chat = futureChat.join();
                var fullResponseWrapper = new Object(){String fullResponse = "";};

                AtomicBoolean queued = new AtomicBoolean(false);
                AtomicLong timeResponseMade = new AtomicLong(System.currentTimeMillis());
                double timeBetween = 1;

                String botUser = character.getType(Character.CharacteristicType.ALIASES).get(0) + ": ";

                chat.filter(chatResp -> chatResp.getChoices() != null && !chatResp.getChoices().isEmpty() && chatResp.firstContent() != null)
                        .map(Chat::firstContent)
                        .forEach(partialResponse -> {
                            fullResponseWrapper.fullResponse += partialResponse;

                            if (!queued.get() && System.currentTimeMillis() - timeResponseMade.get() >= timeBetween && !fullResponseWrapper.fullResponse.isBlank()) {
                                queued.set(true);
                                Consumer<Message> onComplete = newMsg -> {
                                    queued.set(false);
                                    timeResponseMade.set(System.currentTimeMillis());
                                };
                                String newContent = fullResponseWrapper.fullResponse.replaceAll(botUser, "");
                                aiMsg.editMessage(MessageEditData.fromContent(Util.botifyMessage("Message is being streamed: Once the response is complete, this will be gone to let you know the message is done streaming") + "\n" + newContent)).queue(onComplete);
                            }
                        });

                String newContent = fullResponseWrapper.fullResponse.replaceAll(botUser, "");
                aiMsg.editMessage(MessageEditData.fromContent(newContent)).queue();
                this.finishedDiscordResponse(aiMsg);
            } catch (Exception e) {
                aiMsg.editMessage(MessageEditData.fromContent(Util.botifyMessage("Failed to send a response due to an exception :< sowwy.\nError: " + e)))
                        .queue();
                AIBot.bot.getChat(aiMsg.getGuild()).responseFailed();
                throw(e);
            }
        });
    }

    public boolean isMakingResponse() {
        return this.makingResponse;
    }

    public ArrayList<Long> getHistory() {
        return this.history;
    }

    public ArrayList<ChatMessage> getHistoryConverted(){
        ArrayList<ChatMessage> messages = new ArrayList<>(preHistory);

        if(!history.isEmpty()){
            Message firstMsg = null;
            for(long msgId : history){
                try {
                    firstMsg = channel.retrieveMessageById(msgId).submit().get();
                } catch (InterruptedException | ExecutionException ignored) {
                }
                if(firstMsg != null)
                    break;
            }

            if(firstMsg != null){
                OffsetDateTime dateTime = firstMsg.getTimeCreated();
                ArrayList<Message> listOfMessages = null;
                try {
                    listOfMessages = new ArrayList<>(
                            channel.getIterableHistory().deadline(TimeUtil.getDiscordTimestamp(dateTime.getLong(ChronoField.INSTANT_SECONDS))).submit().get()
                    );
                } catch (InterruptedException | ExecutionException ignored) {
                }
                if(listOfMessages != null){
                    listOfMessages.addLast(firstMsg);
                    for(int i = listOfMessages.size() - 1; i >= 0; i--){
                        Message message = listOfMessages.get(i);
                        if(history.contains(message.getIdLong())){
                            String contents = message.getContentRaw();
                            String botUser = character.getType(Character.CharacteristicType.ALIASES).get(0) + ": ";
                            String formatted = contents.replaceAll("<@" + message.getAuthor().getId() + ">", "").replaceAll(botUser, "");

                            if(message.getAuthor() == AIBot.bot.getJDA().getSelfUser()){
                                messages.add(
                                        ChatMessage.AssistantMessage.of(
                                                botUser + formatted
                                        )
                                );
                            } else {
                                String username = message.getAuthor().getGlobalName();
                                messages.add(ChatMessage.UserMessage.of(username+": "+formatted, username));
                            }
                        }
                    }
                }
//                for(long msgId : history){
//                    Message message = null;
//
//                    try {
//                        message = channel.retrieveMessageById(msgId).submit().get();
//                    } catch (InterruptedException | ExecutionException ignored) {
//                    }
//
//                    if(message != null){
//                        String contents = message.getContentRaw();
//                        String formatted = contents.replaceAll("<@" + message.getAuthor().getId() + ">", "");
//
//                        if(message.getAuthor() == AIBot.bot.getJDA().getSelfUser()){
//                            messages.add(
//                                    ChatMessage.AssistantMessage.of(
//                                            character.getType(Character.CharacteristicType.ALIASES).get(0) + ": " + formatted
//                                    )
//                            );
//                        } else {
//                            String username = message.getAuthor().getGlobalName();
//                            messages.add(ChatMessage.UserMessage.of(username+": "+formatted, username));
//                        }
//                    }
//                }
            }
        }

        // ngl we might need a max message count just in case it gets too bigggggg
        Constants.LOGGER.info("Recieved messages! Count: "+messages.size());

        return messages;
    }

    public void restartHistory(){
        history = new ArrayList<>();
        preHistory = new ArrayList<>();
        preHistory.add(instructions.getPrompt());
        preHistory.add(character.getPrompt());
//        history.add(worldLore.getPrompt());
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

    public static Builder builder(Guild guild, Instruction instruction, Character character){
        return new Builder(guild, instruction, character);
    }

    public static class Builder{
        private final Instruction instruction;
        private final Character character;
        private int maxTokens = 8192;
        private String model = Constants.DEFAULT_MODEL;
        private final Guild guild;

        private Builder(Guild guild, Instruction instruction, Character character){
            this.instruction = instruction;
            this.character = character;
            this.guild = guild;
        }

        public void setMaxTokens(int maxTokens){
            this.maxTokens = maxTokens;
        }

        public void setModel(String model){
            this.model = model;
        }

        public DiscordRoleplay build(){
            return new DiscordRoleplay(this);
        }
    }
}
