package discord.mian.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.knuddels.jtokkit.Encodings;
import com.knuddels.jtokkit.api.Encoding;
import com.knuddels.jtokkit.api.EncodingRegistry;
import com.knuddels.jtokkit.api.EncodingType;
import discord.mian.api.Data;
import discord.mian.custom.*;
import discord.mian.data.CharacterData;
import discord.mian.data.InstructionData;
import discord.mian.data.Server;
import discord.mian.data.WorldData;
import discord.mian.interactions.InteractionCreator;
import discord.mian.interactions.Interactions;
import io.github.sashirestela.openai.domain.chat.ChatMessage;
import io.github.sashirestela.openai.domain.chat.ChatRequest;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.actionrow.ActionRowChildComponent;
import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.components.container.Container;
import net.dv8tion.jda.api.components.container.ContainerChildComponent;
import net.dv8tion.jda.api.components.filedisplay.FileDisplay;
import net.dv8tion.jda.api.components.replacer.ComponentReplacer;
import net.dv8tion.jda.api.components.section.Section;
import net.dv8tion.jda.api.components.separator.Separator;
import net.dv8tion.jda.api.components.textdisplay.TextDisplay;
import net.dv8tion.jda.api.components.thumbnail.Thumbnail;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.entities.channel.Channel;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.concrete.ThreadChannel;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.interaction.GenericInteractionCreateEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.GenericComponentInteractionCreateEvent;
import net.dv8tion.jda.api.requests.RestAction;
import net.dv8tion.jda.api.requests.restaction.WebhookMessageCreateAction;
import net.dv8tion.jda.api.utils.FileUpload;
import net.dv8tion.jda.api.utils.TimeUtil;
import net.dv8tion.jda.api.utils.messages.MessageCreateData;
import net.dv8tion.jda.api.utils.messages.MessageEditData;
import okhttp3.*;
import okio.BufferedSource;
import org.apache.tika.Tika;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;

public class Roleplay {
    // limit roleplay to one channel lmfao

    private boolean makingResponse;
    private int maxTokens;
    private double temperature;
    private Model model;
    private String provider;
    private final Guild guild;
    private final Server server;

    private ThreadChannel historyMarker;
    private Webhook webhook;

    private Message latestAssistantMessage;
    private Message errorMsgCleanup;
    private int currentSwipe;
    private ArrayList<ResponseInfo> swipes;

    private final EncodingRegistry registry;

    // make possible to swipe messages
    private HashMap<String, InstructionData> instructions;
    private HashMap<String, WorldData> worldLore;
    private HashMap<String, CharacterData> characters;
    private CharacterData currentCharacter;
    private boolean runningRoleplay = false;

    public Roleplay(Guild guild){
//        this.llm = SimpleOpenAI.builder()
//                .baseUrl(Constants.BASE_URL)
//                .apiKey(Constants.LLM_KEY)
//                .build();

        this.server = AIBot.bot.getServerData(guild);

        HashMap<String, ConfigEntry> configuration = server.getConfig();

        this.setTemperature(configuration.get("temperature").asDouble().value);
        this.setMaxTokens(configuration.get("tokens").asInteger().value);

        String model = configuration.get("model").asString().value;
        String id = model.substring(0, model.indexOf("|"));
        String display = model.substring(model.indexOf("|") + 1);

        this.setModel(new Model(id, display));
        this.setProvider(configuration.get("provider").asString().value);
        this.registry = Encodings.newDefaultEncodingRegistry();
        this.guild = guild;

        instructions = new HashMap<>();
        worldLore = new HashMap<>();
        characters = new HashMap<>();
    }

    public String combinePrompts(List<ChatMessage> msgs){
        String prevType = "";
        StringBuilder combinedText = new StringBuilder();
        for(ChatMessage msg : msgs){
            if(msg instanceof ChatMessage.UserMessage message){
//                if(!prevType.equals("user"))
//                    combinedText.append("USER:");
                combinedText.append("\n").append(message.getContent());
                prevType="user";
            } else if(msg instanceof ChatMessage.AssistantMessage message){
//                if(!prevType.equals("assistant"))
//                    combinedText.append("ASSISTANT:");
                combinedText.append("\n").append(message.getContent());
                prevType="assistant";
            } else if(msg instanceof ChatMessage.SystemMessage message){
                if(!prevType.equals("system"))
                    combinedText.append("SYSTEM:");
                combinedText.append("\n").append(message.getContent());
                prevType="system";
            }
        };
        return combinedText.toString();
    }

    public List<ChatMessage> trimListToMeetTokens(List<ChatMessage> msgs, int startAt){
        String id = this.model.id;
        Encoding enc = registry.getEncodingForModel(id.substring(id.lastIndexOf("/")))
                .orElse(registry.getEncoding(EncodingType.CL100K_BASE));

        String combinedText = combinePrompts(msgs);

        int tokens = enc.countTokens(combinedText.toString());
        if(tokens >= maxTokens){
            int difference = tokens - maxTokens;

            int toRemove = 0;
            int current = 0;
            for(int i = startAt; i < msgs.size() && current < difference; i++){
                ChatMessage msg = msgs.get(i);
                String content = null;
                // inaccurate, we should return maybe the entire json?
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
            List<ChatMessage> newList = msgs.subList(0, startAt);
            newList.addAll(msgs.subList(toRemove + startAt, msgs.size()));
            return newList;
        }

        return msgs;
    }

    public void creatingResponseFromDiscordMessage(){
        if(makingResponse)
            throw new RuntimeException("Already generating a response!");
        this.makingResponse = true;
        if(latestAssistantMessage != null){
            latestAssistantMessage.editMessage(latestAssistantMessage.getContentRaw())
                    .setComponents(List.of()).submit();
        }
    }

    public void finishedDiscordResponse(String finalResponse){
        this.makingResponse = false;
        if(latestAssistantMessage != null && finalResponse != null){
            ArrayList<ActionRowChildComponent> components = new ArrayList<>();
            components.add(InteractionCreator.createPermanentButton(Button.secondary("swipe_left", "<--"), Interactions.getSwipe(Direction.BACK)));
            components.add(InteractionCreator.createPermanentButton(Button.secondary("swipe_right", "-->"), Interactions.getSwipe(Direction.NEXT)));

            if(errorMsgCleanup == null){
                components.add(InteractionCreator.createPermanentButton(Button.secondary("get_provider", Emoji.fromFormatted("❔")), event -> {
                    try{
                        if(!swipes.isEmpty()){
                            ResponseInfo responseInfo = swipes.get(currentSwipe);

                            List<ContainerChildComponent> containerComponents = new ArrayList<>();

                            byte[] data = null;
                            try{
                                data = Files.readAllBytes(currentCharacter.getAvatar().toPath());
                            }catch(Exception e){
                                Constants.LOGGER.error("Failed to get avatar, using backup", e);
                            }
                            data = data != null ? data : Objects.requireNonNull(Util.getRandomImage());
                            Tika tika = new Tika();
                            String type = tika.detect(data);
                            type = type.substring(type.indexOf("/")+1);

                            containerComponents.add(Section.of(
                                    Thumbnail.fromFile(FileUpload.fromData(data, "avatar."+type)),
                                    TextDisplay.of("# Response Information"),
                                    TextDisplay.of("Metadata about the generated response")
                            ));
                            containerComponents.add(Separator.createDivider(Separator.Spacing.SMALL));
                            containerComponents.add(TextDisplay.of("-# The json file sent to the LLM for a response"));
                            containerComponents.add(FileDisplay.fromFile(FileUpload.fromData(responseInfo.getPrompt().getBytes(), "prompt.json")));
                            containerComponents.add(Separator.createDivider(Separator.Spacing.SMALL));

                            String reply = "**Model:** " + responseInfo.getModel() +
                                    "\n**Provider:** " + responseInfo.getProvider() +
                                    "\n**Prompt Tokens:** " + responseInfo.getPromptTokens() +
                                    "\n**Completion Tokens:** " + responseInfo.getCompletionTokens() +
                                    "\n**Total Tokens:** " + responseInfo.getTotalTokens()+
                                    "\n**Price:** " + (responseInfo.getPrice() != null ? "$" + String.format("%.4f", responseInfo.getPrice()) : "Unknown");

                            containerComponents.add(TextDisplay.of(reply));

                            event.deferReply(true)
                                    .useComponentsV2()
                                    .setComponents(Util.createBotContainer(containerComponents))
                                    .queue();
                        } else {
                            throw new RuntimeException("No swipes!");
                        }
                    }catch(Exception e){
                        event.reply("Failed to retrieve response information!").setEphemeral(true).queue();
                        Constants.LOGGER.info(e.toString());
                    }
                }));
                components.add(InteractionCreator.createPermanentButton(Button.danger("destroy", Emoji.fromFormatted("🗑")),
                        Interactions.getDestroyMessage()));
                components.add(InteractionCreator.createPermanentButton(Button.success("edit", Emoji.fromFormatted("🪄")),
                        Interactions.getEditMessage()));
            }

            latestAssistantMessage.editMessage(finalResponse)
                    .setComponents(ActionRow.of(components)).queue();
        }
    }

    public CharacterData findRespondingCharacterFromContent(String content){
        content = content.toLowerCase();
        final String finalContent = content;

        Optional<CharacterData> foundCharacter = server.getCharacterDatas().entrySet().stream().filter(entry ->
                        finalContent.contains(entry.getValue().getFirstName().toLowerCase())).map(entry -> entry.getValue())
                .findFirst();

        if(foundCharacter.isPresent() && getCharacters().containsKey(foundCharacter.get().getName())){
            return foundCharacter.get();
        }
        return null;
    }

    public CharacterData findRespondingCharacterFromMessage(Message msg){
         if(msg.getReferencedMessage() != null && msg.getReferencedMessage().isWebhookMessage()){
            CharacterData character =
                    server.getCharacterDatas().get(msg.getReferencedMessage().getAuthor().getName());

            if(character == null || !getCharacters().containsKey(character.getName())){
                return null;
            }
            return character;
        }
        return null;
    }
//
//    public CompletionRequest createTextRequest(CharacterData character){
//        return CompletionRequest.builder()
//                .maxTokens(this.maxTokens)
//                .model(model.id)
//                .prompt(combinePrompts(getHistory(character)))
//                .temperature(1.0)
//                .build();
//    }

    public ChatRequest createChatRequest(CharacterData character){
        ExtrasChatRequest.ExtrasChatRequestBuilder requestBuilder = ExtrasChatRequest.extrasBuilder()
                .setProviderFallback(false);
        if(provider != null && !provider.isEmpty())
            requestBuilder.setProviders(provider);

        requestBuilder.builder
                .maxCompletionTokens(this.maxTokens)
                .model(model.id)
                .temperature(temperature)
                .stream(true)
                .messages(getHistory(character));

        return requestBuilder.build();
    }

//    public SimpleOpenAI createLLM(){
//        try {
//            return SimpleOpenAI.builder()
//                    .baseUrl(Constants.BASE_URL)
//                    .apiKey(server.getKey())
//                    .build();
//        } catch(Exception e){
//            return null;
//        }
//    }

//    public String createCustomResponse(String content){
//        ArrayList<ChatMessage> history = new ArrayList<>();
//        history.add(ChatMessage.UserMessage.of(content, "Admin"));
//
//        ExtrasChatRequest.ExtrasChatRequestBuilder requestBuilder = ExtrasChatRequest
//                .extrasBuilder()
//                .setProviders()
//                .setProviderFallback(false);
//
//        requestBuilder.builder
//                .maxCompletionTokens(this.maxTokens)
//                .model(this.model)
//                .messages(history);
//
//        ChatRequest chatRequest = requestBuilder.build();
//
//        SimpleOpenAI llm = createLLM();
//
//        if(llm == null){
//            return null;
//        }
//
//        CompletableFuture<Chat> futureChat = llm.chatCompletions()
//                .create(chatRequest);
//        Chat chat = futureChat.join();
//
//        return chat.firstContent();
//    }

    private ResponseInfo generateResponse(CharacterData character, Consumer<String> consumer){
        ChatRequest chatRequest = createChatRequest(character);

        ObjectMapper mapper = new ObjectMapper();
        ObjectWriter writer = mapper.writerWithDefaultPrettyPrinter();
        String json;
        String toSave;
        try{
            json = mapper.writeValueAsString(chatRequest);
            toSave = writer.writeValueAsString(chatRequest);
        }catch(Exception e){
            throw(new RuntimeException(e));
        }

        OkHttpClient client = new OkHttpClient.Builder().build();
        Request request = new Request.Builder()
                .url(Constants.BASE_URL + "/v1/chat/completions")
                .header("Authorization", "Bearer " + server.getKey())
                .header("Content-Type", "application/json")
                .post(RequestBody.create(json, MediaType.get("application/json; charset=utf-8")))
                .build();

        try(Response response = client.newCall(request).execute()) {
            String fullResponse = "";

            if (!response.isSuccessful()) throw new IOException("Unexpected code " + response);

            BufferedSource source = response.body().source();
            String copyOfResponse = "";
            while (!source.exhausted()) {
                String line = source.readUtf8Line();

                if (line.equals("data: [DONE]"))
                    continue;


                if (line.startsWith("data: ")) {
                    if (line.contains("usage"))
                        copyOfResponse = line.substring(6);
                } else {
                    continue;
                }

                if (!line.contains("choices"))
                    continue;

                JsonNode responseJson = mapper.readTree(line.substring(5).trim());
                String content = responseJson.get("choices").get(0).get("delta").get("content").toString();
                content = content.substring(1, content.length() - 1);

                fullResponse += content;
                consumer.accept(fullResponse);
            }

            JsonNode openRouterResponse = mapper.readTree(copyOfResponse);

            return new ResponseInfo(
                    openRouterResponse.get("model").asText(),
                    openRouterResponse.get("provider").asText(),
                    fullResponse,
                    openRouterResponse.get("usage").get("prompt_tokens").asInt(),
                    openRouterResponse.get("usage").get("completion_tokens").asInt(),
                    toSave
            );
        } catch(Exception e){
            Constants.LOGGER.error("Failed to get response from provider", e);
            throw new RuntimeException("Provider failed to give a response! Ensure the key is valid or use a different provider.");
        }
    }

    private ResponseInfo streamOnDiscordMessage(CharacterData character, Message msgToEdit){
        AtomicBoolean queued = new AtomicBoolean(false);
        AtomicLong timeResponseMade = new AtomicLong(System.currentTimeMillis());
        double timeBetween = 1;

        String botUser = character.getName() + ":";
        Function<String, String> reformat = (string) ->
            string.replace(botUser, "")
                    .replace("\\n", "")
                    .replace("\\r", "")
                    .replace("\\", "");

        ResponseInfo responseInfo = generateResponse(character, response -> {
            if (!queued.get() && System.currentTimeMillis() - timeResponseMade.get() >= timeBetween && !response.isBlank()) {
                queued.set(true);
                Consumer<Message> onComplete = newMsg -> {
                    queued.set(false);
                    timeResponseMade.set(System.currentTimeMillis());
                };
                String newContent = reformat.apply(response);
                msgToEdit.editMessage(MessageEditData.fromContent(Util.botifyMessage("Message is currently generating..") + "\n" + newContent)).queue(onComplete);
            }
        });
        if(responseInfo == null)
            throw new RuntimeException("Failed to create the LLM! Ensure that the API key is valid!");
        String newContent = responseInfo.editResponse(reformat);
        if(newContent.isEmpty())
            throw new RuntimeException("Empty message! :(");
        return responseInfo;
    }

    public void promptCharacterToRoleplay(CharacterData character, Message replyTo, boolean triggerAutoResponse, boolean waitForFinish) throws ExecutionException, InterruptedException {
        if(isRunningRoleplay()){
            if(!characters.containsKey(character.getName())){
                Message message = historyMarker.retrieveParentMessage().submit().get();
                Container container = message.getComponentTree().getComponents().getFirst().asContainer();
                TextDisplay charactersDisplay = container.getComponents().stream().filter(component -> component.getUniqueId() == 152)
                                .findFirst().get().asTextDisplay();
                message.editMessageComponents(container.replace(ComponentReplacer.byId(152, charactersDisplay.withContent(
                        charactersDisplay.getContent() + ", " + character.getName()
                )))).useComponentsV2().submit().get();
                // adds the character to the container

                addData(PromptType.CHARACTER, character);
            }

            setCurrentCharacter(character.getName());
            sendRoleplayMessage(triggerAutoResponse, waitForFinish);
        }
    }

    // selects a random character based on their talking likeliness
    public void selectRandomCharacter(){

    }

    public void sendRoleplayMessage(boolean triggerAutoResponse, boolean waitForFinish) throws ExecutionException, InterruptedException {
        if(!runningRoleplay){
            historyMarker.sendMessage(MessageCreateData.fromContent(
                    Util.botifyMessage("Cannot make a response since there is no ongoing chat!")
            )).queue();
            return;
        }
        if (isMakingResponse()) {
            historyMarker.sendMessage(MessageCreateData.fromContent(
                    Util.botifyMessage("Cannot make a response since I am already generating one!")
            )).queue();
            return;
        }
        if(currentCharacter == null){
            historyMarker.sendMessage(MessageCreateData.fromContent(
                    Util.botifyMessage("Cannot make a response since there are no characters in this chat!")
            )).queue();
            return;
        }

        if(this.errorMsgCleanup != null){
            this.errorMsgCleanup.delete().queue(RestAction.getDefaultSuccess(), toThrow -> {});
            if(this.latestAssistantMessage == errorMsgCleanup)
                latestAssistantMessage = null;
            this.errorMsgCleanup = null;
        }

        if(this.latestAssistantMessage != null){
            if(latestAssistantMessage.getContentRaw().isEmpty())
                latestAssistantMessage.delete().queue();
            else
                latestAssistantMessage.editMessageComponents(
                        ActionRow.of(Button.danger("destroy_button", Emoji.fromFormatted("🗑")),
                                Button.success("edit_button", Emoji.fromFormatted("🪄")))).queue(RestAction.getDefaultSuccess(),
                        (t) -> {});
            latestAssistantMessage = null;
            swipes = null;
            currentSwipe = 0;
        }

        try{
            this.creatingResponseFromDiscordMessage();

            String avatarLink = null;
            try{
                avatarLink = currentCharacter.getAvatarLink();
            } catch (Exception e) {
                Constants.LOGGER.error("Failed to get character avatar link", e);
            }

            WebhookMessageCreateAction<Message> messageCreateData = webhook.sendMessage(
                            Util.botifyMessage("Currently creating a response! Check back in a second.."))
                    .setThread(historyMarker)
                    .setUsername(currentCharacter.getName());

            if(avatarLink != null){
                messageCreateData = messageCreateData.setAvatarUrl(avatarLink);
            }

            Consumer<Message> consumer = (aiMsg) -> {
                latestAssistantMessage = aiMsg;
                swipes = new ArrayList<>();
                try {
                    ResponseInfo responseInfo = streamOnDiscordMessage(currentCharacter, aiMsg);
                    swipes.add(responseInfo);
                    errorMsgCleanup = null;
                    this.finishedDiscordResponse(responseInfo.getResponse());

                    // if able to reply to other bots..
                    // add chaining
                    if(triggerAutoResponse){
                        CharacterData data = findRespondingCharacterFromContent(responseInfo.getResponse());
                        if(data != null && data != currentCharacter && data.getTalkability() >= Math.random()) {
                            try{
                                promptCharacterToRoleplay(data, latestAssistantMessage, true, waitForFinish);
                            } catch (Exception e){
                                Constants.LOGGER.error("Failed to prompt a response", e);
                            }
                        }
                    }
                } catch (Exception e) {
                    errorMsgCleanup = aiMsg;
                    throw(e);
                }
            };

            if(waitForFinish){
                consumer.accept(messageCreateData.complete());
            } else {
                messageCreateData.queue(consumer);
            }
        }catch(Exception e){
            String overrideError = null;
            if(e instanceof IllegalArgumentException ignored){
                overrideError = "Response is too long!";
            }

            currentSwipe = 0;
            this.finishedDiscordResponse(Util.botifyMessage("Failed to send a response due to an exception :< sowwy. If this keeps happening, try using a different AI model or provider.\n\nError: " + (overrideError != null ? overrideError : e.toString().substring(0, Math.min(e.toString().length(), 1750)))));
            throw(e);
        }
    }

    public void swipe(ButtonInteractionEvent event, Direction direction){
        if(latestAssistantMessage != null){
            if(event.getMessage().getIdLong() != latestAssistantMessage.getIdLong()){
                event.getHook().editOriginal("You cannot swipe on this message anymore :(, consider editing it instead!")
                        .queue();
                return;
            }

            String finalResponse = null;
            if(direction == Direction.BACK){
                currentSwipe--;
            } else {
                if(currentSwipe + 1 >= swipes.size()){
                    if (isMakingResponse()) {
                        event.getHook().editOriginal("Cannot make a response since I am already generating one!")
                                .queue();
                        return;
                    }
                    CharacterData character = characters.get(latestAssistantMessage.getAuthor().getName());
                    this.creatingResponseFromDiscordMessage();

                    try{
                        ResponseInfo responseInfo = streamOnDiscordMessage(character, latestAssistantMessage);
                        finalResponse = responseInfo.getResponse();
                        currentSwipe++;
                        swipes.add(responseInfo);
                        errorMsgCleanup = null;
                    }catch(Exception e){
                        String msg = e.toString().contains("Content may not be longer") ? "Content is too long!" : e.toString();
                        finalResponse = Util.botifyMessage("Failed to make a new response!\n\nError: " + msg);
                    }
                    this.finishedDiscordResponse(finalResponse);
                } else {
                    currentSwipe++;
                }
            }

            if(currentSwipe >= swipes.size())
                currentSwipe = swipes.isEmpty() ? 0 : swipes.size() - 1;

            if(currentSwipe <= -1)
                currentSwipe = 0;

            if(!swipes.isEmpty())
                latestAssistantMessage.editMessage(finalResponse != null ? MessageEditData.fromContent(finalResponse) :
                        MessageEditData.fromContent(swipes.get(currentSwipe).getResponse())).queue();
        }
    }

    public boolean isMakingResponse() {
        return this.makingResponse;
    }

    public boolean isRunningRoleplay(){
        return runningRoleplay;
    }

    public List<ChatMessage> getHistory(){
        return getHistory(null);
    }

    // beginning prompts not included if no character is provided
    public List<ChatMessage> getHistory(CharacterData character){
        if(historyMarker == null)
            return new ArrayList<>();

        ArrayList<ChatMessage> messages = new ArrayList<>();

        if(character != null){
            StringBuilder instructionsMessage = new StringBuilder();
            instructionsMessage.append("Follow the instructions below! You are participating in a roleplay with other users!\n");
            instructionsMessage.append("This is a chatbot roleplay. You are roleplaying with other users, your responses should only be a few sentences long, should incorporate humor and shouldn't be too serious. The only time this can be overridden is if later instructions conflict with these. \nKeep responses within a few sentences!\nDo not escape newlines or quotes in your response. Respond with actual characters, not \\\\n or \\\\\\\". Discord will display it properly.\n");
            instructionsMessage.append("Each user message has a name field. Use this to determine who is speaking and maintain consistency");
            instructionsMessage.append("Do not include the character name in your response, this is already provided programmatically by the code.\n");

            for(InstructionData instructionData : instructions.values()){
                instructionsMessage.append(instructionData.getChatMessage(character).getContent()).append("\n");
            }

            messages.add(ChatMessage.SystemMessage.of(instructionsMessage.toString(), "Instructions"));

            StringBuilder combinedLore = new StringBuilder();
            combinedLore.append("The following is lore and information about the world that this roleplay takes place in!");
            for(WorldData worldData : worldLore.values()){
                combinedLore.append(worldData.getChatMessage(character).getContent()).append("\n");
            }
            messages.add(ChatMessage.SystemMessage.of(combinedLore.toString(), "Lore"));

//            StringBuilder multipleCharacters = new StringBuilder("For your response, you will be replying as {{char}}. Do not respond as any of the other characters in this group except {{char}}: ");
//            for(String name : characters.keySet()){
//                multipleCharacters.append(name).append(", ");
//            };
//            multipleCharacters.replace(multipleCharacters.lastIndexOf(", "), multipleCharacters.length(), ".");
//
//            messages.add(
//                    ChatMessage.SystemMessage.of(multipleCharacters.toString())
//            );
            StringBuilder characterPersona = new StringBuilder();
            characterPersona.append("Understand the character definition below! This is the character you will be playing in the roleplay.\n");
            characterPersona.append(character.getChatMessage(character).getContent());
            messages.add(ChatMessage.SystemMessage.of(characterPersona.toString(), "CharacterDefinition"));
            messages.add(ChatMessage.SystemMessage.of("<CHAT HISTORY>"));
        }
        int required = messages.size();

//        Message start = null;
        ArrayList<Message> listOfMessages = new ArrayList<>();
        try {
//            start = historyMarker.retrieveStartMessage().submit().get();
            listOfMessages = new ArrayList<>(historyMarker.getIterableHistory().submit().get());
        } catch (InterruptedException | ExecutionException e) {
            Constants.LOGGER.error("Failed to get start of message history", e);
        }

        for(int i = listOfMessages.size() - 1; i >= 0; i--) {
            Message message = listOfMessages.get(i);
//            if(message.getIdLong() == start.getIdLong())
//                continue;
            if(message.getAuthor() == AIBot.bot.getJDA().getSelfUser())
                continue;
            if(message.getContentRaw().contains("Currently creating a response"))
                continue;
            if(latestAssistantMessage != null &&
                    (latestAssistantMessage.getIdLong() == message.getIdLong() ||
                            latestAssistantMessage.getTimeCreated().isBefore(message.getTimeCreated())))
                continue;

            String contents = message.getContentRaw();
            String username = message.getAuthor().getGlobalName();
            if (username == null)
                username = message.getAuthor().getName();

            String formatted = contents
                    .replaceAll("<@" + message.getAuthor().getId() + ">", "")
                    .replaceAll("<|im_end|>", "");

            if (message.isWebhookMessage()) {
                messages.add(
                        ChatMessage.AssistantMessage.builder()
                                .content(username +": "+ formatted)
                                .name(username)
                                .build()
                );
            } else {
                messages.add(ChatMessage.UserMessage.of(username+": "+formatted, username));
            }
        }
        if(character != null)
            messages.add(ChatMessage.SystemMessage.of("Write as " + character.getName() + " for your next response!"));
        return trimListToMeetTokens(messages, required);
    }

    public void startRoleplay(GenericComponentInteractionCreateEvent event, List<InstructionData> instructionList, List<WorldData> worldDatas, List<CharacterData> characterList) throws ExecutionException, InterruptedException, IOException {
        if(instructionList.size() <= 0)
            throw new RuntimeException("Need at least one set of instructions!");
        if(worldDatas.size() <= 0)
            throw new RuntimeException("Need at least one set of world lore!");

        event.deferReply().useComponentsV2().queue();

        List<ContainerChildComponent> components = new ArrayList<>();

        components.add(TextDisplay.of("# Roleplay Information"));
        components.add(TextDisplay.of("The roleplay starts in the thread created under this message, have fun!"));
        components.add(Separator.createDivider(Separator.Spacing.SMALL));
        components.add(TextDisplay.of("**Chat Messages:** "+ 0).withUniqueId(100)); // chat messages marker
        components.add(Separator.createDivider(Separator.Spacing.SMALL));

        AtomicInteger uniqueId = new AtomicInteger(150);

        Function<PromptType, List<? extends Data>> getDatas = (promptType) ->
                switch(promptType){
                    case INSTRUCTION -> instructionList;
                    case WORLD -> worldDatas;
                    case CHARACTER -> characterList;
                };

        for(PromptType promptType : PromptType.values()){
            List<? extends Data> promptDatas = getDatas.apply(promptType);

            StringBuilder display = new StringBuilder();
            components.add(TextDisplay.of("**"+promptType.displayName+" Involved"+"**"));

            for(int i = 0; i < promptDatas.size(); i++){
                String name = promptDatas.get(i).getName();
                display.append(name);
                if(i != promptDatas.size() - 1){
                    display.append(", ");
                }
            }
            components.add(TextDisplay.of(display.toString()).withUniqueId(uniqueId.getAndIncrement()));
        }

        components.add(Separator.createDivider(Separator.Spacing.SMALL));
        components.add(ActionRow.of(InteractionCreator.createPermanentButton(Button.primary("start_here", "Start Here"),
                button -> {
            button.deferReply(true).queue();
            try{
                startRoleplay(button.getMessage().getChannelType().isThread() ?
                        button.getChannel().asThreadChannel() :
                        button.getMessage().getStartedThread());
            }catch(Exception e){
                button.getHook().editOriginal("Failed to restart roleplay!").queue();
                Constants.LOGGER.error("Failed to start roleplay", e);
                return;
            }
            button.getHook().editOriginal("Successfully started roleplay!").queue();
        })
                .withEmoji(Emoji.fromFormatted("🔁"))));

         Message message = event.getHook().editOriginalComponents(Util.createBotContainer(components))
                 .useComponentsV2()
                 .complete();
         startRoleplay(message.createThreadChannel("Roleplay").complete());
    }

    public void startRoleplay(ThreadChannel historyMarker){
        try{
            if(isRunningRoleplay())
                stopRoleplay();

            Message roleplayInfo = historyMarker.retrieveParentMessage().complete();

            TextChannel channel = historyMarker.getParentChannel().asTextChannel();
            Webhook webhook;
            webhook = channel.retrieveWebhooks().complete().stream().filter(find -> find.getName().equals(AIBot.bot.getJDA().getSelfUser().getName()))
                    .findFirst().orElse(null);
            if(webhook == null){
                if(AIBot.bot.getJDA().getSelfUser().getAvatar() != null){
                    InputStream inputStream = AIBot.bot.getJDA().getSelfUser().getAvatar().download().get();
                    webhook = channel.createWebhook(AIBot.bot.getJDA().getSelfUser().getName())
                            .setAvatar(Icon.from(inputStream)).complete();
                } else {
                    webhook = channel.createWebhook(AIBot.bot.getJDA().getSelfUser().getName())
                            .complete();
                }
            }

            this.webhook = webhook;

            runningRoleplay = true;

            latestAssistantMessage = null;
            swipes = null;
            currentSwipe = 0;

            this.characters.clear();
            this.instructions.clear();
            this.worldLore.clear();

            AtomicInteger uniqueId = new AtomicInteger(150);
            for(PromptType promptType : PromptType.values()){
                Container container = roleplayInfo.getComponentTree().getComponents().getFirst().asContainer();
                container.getComponents().stream().filter(component -> component.getUniqueId() == uniqueId.get())
                        .findFirst().ifPresent(component -> {
                            uniqueId.incrementAndGet();
                            TextDisplay display = component.asTextDisplay();
                            List<String> prompts = List.of(display.getContent().split(", "));
                            prompts.forEach(name -> {
                                Data data = server.getDatas(promptType).get(name);
                                if(data != null)
                                    addData(promptType, data);
                            });
                        });
            }

            Stream<String> stream = characters.keySet().stream();
            stream.findAny().ifPresent(this::setCurrentCharacter);

            this.historyMarker = historyMarker;
        } catch(Exception e){
            runningRoleplay = false;
            this.characters.clear();
            this.instructions.clear();
            this.worldLore.clear();
            setCurrentCharacter(null);

            throw new RuntimeException(e);
        }
    }

    public void stopRoleplay(){
        if(this.isRunningRoleplay()){
            runningRoleplay = false;
            historyMarker = null;
            webhook = null;
            latestAssistantMessage = null;
            swipes = null;
            currentSwipe = 0;
            characters.clear();
            instructions.clear();
            worldLore.clear();
        }
    }

    public int getCurrentSwipe(){
        return currentSwipe;
    }

    public ArrayList<ResponseInfo> getSwipes(){
        return swipes;
    }

    public void setMaxTokens(int maxTokens){
        HashMap<String, ConfigEntry> config = server.getConfig();
        config.get("tokens").asInteger().value = maxTokens;
        server.saveToConfig(config);

        this.maxTokens = maxTokens;
    }

    public void setModel(Model model){
        HashMap<String, ConfigEntry> config = server.getConfig();
        config.get("model").asString().value = model.toString();
        server.saveToConfig(config);

        this.model = model;
    }

    public void setTemperature(double temperature){
        HashMap<String, ConfigEntry> config = server.getConfig();
        config.get("temperature").asDouble().value = temperature;
        server.saveToConfig(config);

        this.temperature = Math.max(0, Math.min(temperature, 2));
    }

    public void setProvider(String provider){
        HashMap<String, ConfigEntry> config = server.getConfig();
        config.get("provider").asString().value = provider;
        server.saveToConfig(config);

        this.provider = provider;
    }

    public double getTemperature(){
        return temperature;
    }

    public int getMaxTokens(){
        return maxTokens;
    }

    public String getProvider(){
        return provider;
    }

    public Model getModel(){
        return model;
    }

    private HashMap<String, WorldData> getWorlds(){
        return worldLore;
    }

    private HashMap<String, InstructionData> getInstructions() {
        return instructions;
    }

    private HashMap<String, CharacterData> getCharacters() {
        return characters;
    }

    public List<? extends Data> getDatas(PromptType promptType){
        return switch(promptType){
            case CHARACTER -> getCharacters().values().stream().toList();
            case WORLD -> getWorlds().values().stream().toList();
            case INSTRUCTION -> getInstructions().values().stream().toList();
        };
    }

    public CharacterData getCurrentCharacter(){
        return currentCharacter;
    }

    public ThreadChannel getChannel(){
        return historyMarker;
    }

    public Message getLatestAssistantMessage(){
        return latestAssistantMessage;
    }

    public void setCurrentCharacter(String name){
        currentCharacter = characters.get(name);
    }

    public void addData(PromptType type, Data data){
        if(!server.getDatas(type).containsKey(data.getName()))
            throw new RuntimeException(data.getName() + " is no longer a valid data!");
        switch(type){
            case CHARACTER -> {
                characters.putIfAbsent(data.getName(), (CharacterData) data);
                currentCharacter = (CharacterData) data;
            }
            case WORLD -> worldLore.putIfAbsent(data.getName(), (WorldData) data);
            case INSTRUCTION -> instructions.putIfAbsent(data.getName(), (InstructionData) data);
        }
    }
}
