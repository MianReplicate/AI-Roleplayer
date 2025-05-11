package discord.mian.ai;

import com.knuddels.jtokkit.Encodings;
import com.knuddels.jtokkit.api.Encoding;
import com.knuddels.jtokkit.api.EncodingRegistry;
import com.knuddels.jtokkit.api.EncodingType;
import discord.mian.ai.data.CharacterData;
import discord.mian.ai.data.InstructionData;
import discord.mian.custom.Direction;
import discord.mian.custom.Util;
import discord.mian.custom.Constants;
import io.github.sashirestela.openai.SimpleOpenAI;
import io.github.sashirestela.openai.domain.chat.Chat;
import io.github.sashirestela.openai.domain.chat.ChatMessage;
import io.github.sashirestela.openai.domain.chat.ChatRequest;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.requests.restaction.WebhookMessageCreateAction;
import net.dv8tion.jda.api.utils.TimeUtil;
import net.dv8tion.jda.api.utils.messages.MessageCreateData;
import net.dv8tion.jda.api.utils.messages.MessageEditData;

import java.io.IOException;
import java.io.InputStream;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoField;
import java.util.ArrayList;
import java.util.HashMap;
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
    private Webhook webhook;

    // need to create a new arraylist that stores chats
    private ArrayList<Long> history;

    private Message latestAssistantMessage;
    private int currentSwipe;
    private ArrayList<String> swipes;

    private final EncodingRegistry registry;

    // make possible to swipe messages
    private ArrayList<InstructionData> instructions;
    private HashMap<String, CharacterData> characters;
    private CharacterData currentCharacter;
//    private String funnyMessage;
//    private final WorldLore worldLore;
    private boolean runningRoleplay = false;

    public DiscordRoleplay(Guild guild){
        if(Constants.LLM_KEY == null || Constants.LLM_KEY.isEmpty())
            throw new RuntimeException("No OpenRouter key set!");

        this.llm = SimpleOpenAI.builder()
                .baseUrl(Constants.BASE_URL)
                .apiKey(Constants.LLM_KEY)
                .build();

        this.maxTokens = 8192;
        this.model = Constants.DEFAULT_MODEL;
        this.registry = Encodings.newDefaultEncodingRegistry();
        this.guild = guild;
        this.restartHistory();
    }

    public void trimHistoryIfNeeded(CharacterData character){
        Encoding enc = registry.getEncodingForModel(this.model.substring(this.model.lastIndexOf("/")))
                .orElse(registry.getEncoding(EncodingType.CL100K_BASE));

        StringBuilder combinedText = new StringBuilder();
        ArrayList<ChatMessage> msgs = getHistoryConverted(character);
        for(ChatMessage msg : msgs){
            if(msg instanceof ChatMessage.UserMessage message){
                combinedText.append(message.getContent());
            } else if(msg instanceof ChatMessage.AssistantMessage message){
                combinedText.append(message.getContent());
            } else if(msg instanceof ChatMessage.SystemMessage message){
                combinedText.append(message.getContent());
            }
        };

        int tokens = enc.countTokens(combinedText.toString());
        if(tokens >= maxTokens){
            int difference = tokens - maxTokens;

            int toRemove = 0;
            int current = 0;
            for(int i = 0; i < msgs.size() && current < difference; i++){
                ChatMessage msg = msgs.get(i);
                String content = null;
                if(msg instanceof ChatMessage.UserMessage message){
                    content = message.getContent().toString();
                } else if(msg instanceof ChatMessage.AssistantMessage message){
                    content = message.getContent().toString();
                } else if(msg instanceof ChatMessage.SystemMessage message){
                    content = message.getContent().toString();
                }
                current += enc.countTokens(content);
                toRemove++;
            }
            history = new ArrayList<>(history.subList(toRemove, history.size()));
        }
    }

    public void creatingResponseFromDiscordMessage(CharacterData character){
        if(makingResponse)
            throw new RuntimeException("Already generating a response!");
        this.trimHistoryIfNeeded(character);
        this.makingResponse = true;
        if(latestAssistantMessage != null){
            latestAssistantMessage.editMessage(latestAssistantMessage.getContentRaw())
                    .setComponents(List.of()).submit();
        }
    }

    public void finishedDiscordResponse(String finalResponse){
//        this.history.add(message.getIdLong());
        this.makingResponse = false;
        if(latestAssistantMessage != null && finalResponse != null){
            latestAssistantMessage.editMessage(finalResponse)
                    .setActionRow(
                            Button.primary("back_swipe", "<--"),
                            Button.primary("next_swipe", "-->"),
                            Button.danger("destroy", Emoji.fromFormatted("🗑")),
                            Button.success("edit", Emoji.fromFormatted("🪄")
                            )).queue();
        }
    }

    public ChatRequest createRequest(CharacterData character){
        return ChatRequest.builder()
                .maxCompletionTokens(this.maxTokens)
                .model(this.model)
                .messages(getHistoryConverted(character))
                .temperature(1.0)
                .build();
    }

    public String createCustomResponse(String content){
        ArrayList<ChatMessage> history = new ArrayList<>();
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

    private String generateResponse(CharacterData character, Consumer<String> consumer){
        ChatRequest chatRequest = createRequest(character);
        CompletableFuture<Stream<Chat>> futureChat = this.llm.chatCompletions()
                .createStream(chatRequest);
        Stream<Chat> chat = futureChat.join();
        var fullResponseWrapper = new Object(){String fullResponse = "";};

        chat.filter(chatResp -> chatResp.getChoices() != null && !chatResp.getChoices().isEmpty() && chatResp.firstContent() != null)
                .map(Chat::firstContent)
                .forEach(partialResponse -> {
                    fullResponseWrapper.fullResponse += partialResponse;
                    consumer.accept(fullResponseWrapper.fullResponse);
                });
        return fullResponseWrapper.fullResponse;
    }

    private String streamOnDiscordMessage(CharacterData character, Message msgToEdit){
        AtomicBoolean queued = new AtomicBoolean(false);
        AtomicLong timeResponseMade = new AtomicLong(System.currentTimeMillis());
        double timeBetween = 1;

        String botUser = character.getName() + ": ";
        String finalResponse = generateResponse(character,response -> {
            if (!queued.get() && System.currentTimeMillis() - timeResponseMade.get() >= timeBetween && !response.isBlank()) {
                queued.set(true);
                Consumer<Message> onComplete = newMsg -> {
                    queued.set(false);
                    timeResponseMade.set(System.currentTimeMillis());
                };
                String newContent = response.replaceAll(botUser, "");
                msgToEdit.editMessage(MessageEditData.fromContent(Util.botifyMessage("Message is being streamed: Once the response is complete, this will be gone to let you know the message is done streaming") + "\n" + newContent)).queue(onComplete);
            }
        });
        String newContent = finalResponse.replaceAll(botUser, "");
        if(newContent.isEmpty())
            throw new RuntimeException("Empty message! :(");
        return newContent;
    }

    public void sendRoleplayMessage(Message userMsg) {
        if(!runningRoleplay){
            userMsg.getChannel().sendMessage(MessageCreateData.fromContent(
                    Util.botifyMessage("Cannot make a response since there is no ongoing chat!")
            )).queue();
            return;
        }
        if (isMakingResponse()) {
            userMsg.getChannel().sendMessage(MessageCreateData.fromContent(
                    Util.botifyMessage("Cannot make a response since I am already generating one!")
            )).queue();
            return;
        }
        if(currentCharacter == null){
            userMsg.getChannel().sendMessage(MessageCreateData.fromContent(
                    Util.botifyMessage("Cannot make a response since there are no characters in this chat!")
            )).queue();
            return;
        }

        if(this.latestAssistantMessage != null){
            history.add(latestAssistantMessage.getIdLong());
            if(latestAssistantMessage.getContentRaw().isEmpty())
                latestAssistantMessage.delete().queue();
            else
                latestAssistantMessage.editMessageComponents(
                        ActionRow.of(Button.danger("destroy", Emoji.fromFormatted("🗑")),
                                Button.success("edit", Emoji.fromFormatted("🪄")))).queue();
            latestAssistantMessage = null;
            swipes = null;
            currentSwipe = 0;
        }
        this.creatingResponseFromDiscordMessage(currentCharacter);
        this.history.add(userMsg.getIdLong());

        String avatarLink = null;
        try{
            avatarLink = currentCharacter.getAvatarLink();
        } catch (Exception ignored) {

        }

        WebhookMessageCreateAction<Message> messageCreateData = webhook.sendMessage(
                Util.botifyMessage("Currently creating a response! Check back in a second.."))
                .setUsername(currentCharacter.getName());

        if(avatarLink != null){
            messageCreateData = messageCreateData.setAvatarUrl(avatarLink);
        }

        messageCreateData.queue(aiMsg -> {
            try {
                String finalResponse = streamOnDiscordMessage(currentCharacter, aiMsg);
                latestAssistantMessage = aiMsg;
                swipes = new ArrayList<>();
                swipes.add(finalResponse);
                this.finishedDiscordResponse(finalResponse);
            } catch (Exception e) {
                this.finishedDiscordResponse(null);
                aiMsg.editMessage(MessageEditData.fromContent(Util.botifyMessage("Failed to send a response due to an exception :< sowwy. Try using a different AI model.\nError: " + e.toString().substring(0, Math.min(e.toString().length(), 1925)))))
                        .queue();
                throw(e);
            }
        });
    }

    public boolean swipe(ButtonInteractionEvent event, Direction direction){
        if(latestAssistantMessage != null){
            if(event.getMessage().getIdLong() != latestAssistantMessage.getIdLong()){
                event.getHook().editOriginal("You cannot swipe on this message anymore :(, consider editing it instead!")
                        .queue();
                return false;
            }

            if(direction == Direction.BACK){
                currentSwipe--;
                if(currentSwipe == -1)
                    currentSwipe = Math.min(0, swipes.size() - 1);
            } else {
                if(currentSwipe + 1 >= swipes.size()){
                    if (isMakingResponse()) {
                        event.getHook().editOriginal("Cannot make a response since I am already generating one!")
                                .queue();
                        return false;
                    }
                    CharacterData character = characters.get(latestAssistantMessage.getAuthor().getName());
                    this.creatingResponseFromDiscordMessage(character);

                    String finalResponse;
                    try{
                        finalResponse = streamOnDiscordMessage(character, latestAssistantMessage);
                        currentSwipe++;
                        swipes.add(finalResponse);
                    }catch(Exception e){
                        event.getHook().editOriginal("Failed to make a new response!")
                                .queue();
                        throw(e);
                    }
                    this.finishedDiscordResponse(finalResponse);
                } else {
                    currentSwipe++;
                    if(currentSwipe >= swipes.size())
                        currentSwipe = 0;
                }
            }
            latestAssistantMessage.editMessage(MessageEditData.fromContent(swipes.get(currentSwipe))).queue();
        }
        return true;
    }

    public boolean isMakingResponse() {
        return this.makingResponse;
    }

    public ArrayList<Long> getHistory() {
        return this.history;
    }

    public boolean isRunningRoleplay(){
        return runningRoleplay;
    }

    public ArrayList<ChatMessage> getHistoryConverted(CharacterData character){
        ArrayList<ChatMessage> messages = new ArrayList<>();

        messages.add(ChatMessage.SystemMessage.of("<INSTRUCTIONS>\nFollow the instructions below! You are the Game Master and will follow these rules!", "INSTRUCTIONS"));
        for(InstructionData instructionData : instructions){
            messages.add(instructionData.getChatMessage(character));
        }
        messages.add(ChatMessage.SystemMessage.of("<CHARACTER>\nUnderstand the character definition below! This is the character you will be playing in the roleplay.", "CHARACTER"));
        messages.add(character.getChatMessage(character));

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
                            String username = message.getAuthor().getGlobalName();

                            String formatted = contents.replaceAll("<@" + message.getAuthor().getId() + ">", "");

                            if(message.getAuthor() == AIBot.bot.getJDA().getSelfUser()){
                                messages.add(
                                        ChatMessage.AssistantMessage.of(
                                                username +": " + formatted
                                        )
                                );
                            } else {
                                messages.add(ChatMessage.UserMessage.of(username+": "+formatted, username));
                            }
                        }
                    }
                }
            }
        }

        // ngl we might need a max message count just in case it gets too bigggggg
        Constants.LOGGER.info("Recieved messages! Count: "+messages.size());

        return messages;
    }

    public void startRoleplay(TextChannel channel, InstructionData instructions, List<CharacterData> characterList) throws ExecutionException, InterruptedException, IOException {
        if(instructions == null)
            throw new RuntimeException("No initial instructions given!");

        this.channel = channel;

        Webhook webhook;
        webhook = channel.retrieveWebhooks().submit().get().stream().filter(find -> find.getName().equals(AIBot.bot.getJDA().getSelfUser().getName()))
                .findFirst().orElse(null);
        if(webhook == null){
            InputStream inputStream = AIBot.bot.getJDA().getSelfUser().getAvatar().download().get();
            webhook = channel.createWebhook(AIBot.bot.getJDA().getSelfUser().getName())
                    .setAvatar(Icon.from(inputStream)).submit().get();
        }
        this.webhook = webhook;

        runningRoleplay = true;
        restartHistory();
        characters = new HashMap<>();
        characterList.forEach(this::addCharacter);
        this.instructions = new ArrayList<>();

        addInstructions(instructions);
        Stream<String> stream = characters.keySet().stream();
        stream.findAny().ifPresent(this::setCurrentCharacter);
    }

    public void restartHistory(){
        history = new ArrayList<>();

        latestAssistantMessage = null;
        currentSwipe = 0;
        swipes = null;
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

    public HashMap<String, CharacterData> getCharacters() {
        return characters;
    }

    public CharacterData getCurrentCharacter(){
        return currentCharacter;
    }

    public TextChannel getChannel(){
        return channel;
    }

    public void setCurrentCharacter(String name){
        currentCharacter = characters.get(name);
    }

    public void addCharacter(CharacterData character){
        characters.putIfAbsent(character.getName(), character);
        if(currentCharacter == null)
            currentCharacter = character;
    }

    public void addInstructions(InstructionData instructionData){
        this.instructions.add(instructionData);
    }
}
