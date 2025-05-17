package discord.mian.commands.custom;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import discord.mian.ai.AIBot;
import discord.mian.ai.Model;
import discord.mian.ai.Roleplay;
import discord.mian.commands.SlashCommand;
import discord.mian.custom.Util;
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.InteractionContextType;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyCallbackAction;
import okhttp3.Call;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class ChangeModel extends SlashCommand {
    public ChangeModel(){
        super("model", "Change the LLM being used");
        this.addOption(OptionType.STRING, "name", "The model to use", true, true);
        this.setContexts(InteractionContextType.GUILD);
    }

    @Override
    public boolean handle(SlashCommandInteractionEvent event) throws Exception {
        String id = event.getOption("name", OptionMapping::getAsString);

        Map<String, String> validModels = Map.of();
        OkHttpClient client = new OkHttpClient.Builder().build();

        Request request = new Request.Builder()
                .url("https://openrouter.ai/api/v1/models")
                .get()
                .build();

        Call call = client.newCall(request);
        try (Response response = call.execute()) {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode node = mapper.readTree(response.body().string());
            JsonNode dataNode = node.get("data");

            validModels = dataNode.valueStream().collect(Collectors.toMap(
                    model -> model.get("id").asText(),
                    model -> model.get("name").asText()
            ));
        }

        if(!validModels.containsKey(id)){
            event.reply("Not a valid model on OpenRouter!").setEphemeral(true).queue();
            return true;
        }

        Map<String, String> finalValidModels = validModels;

        Roleplay roleplay = AIBot.bot.getChat(event.getGuild());
        Consumer<InteractionHook> consumer = (interactionHook) -> roleplay
                .setModel(new Model(id, finalValidModels.get(id)));

        HashMap<String, Double> endpoints = ChangeProvider.getEndpoints(id);
        if(roleplay.getProvider() != null && !roleplay.getProvider().isEmpty() && !endpoints.containsKey(roleplay.getProvider())){
            roleplay.setProvider(null); // invalid provider!
        }

        ReplyCallbackAction reply = event.reply("Changed model!").setEphemeral(true);
        reply.queue(consumer);
        return true;
    }

    @Override
    public void autoComplete(CommandAutoCompleteInteractionEvent event){
        OkHttpClient client = new OkHttpClient.Builder().build();

        Request request = new Request.Builder()
                .url("https://openrouter.ai/api/v1/models")
                .get()
                .build();

        Call call = client.newCall(request);
        try (Response response = call.execute()) {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode node = mapper.readTree(response.body().string());
            JsonNode dataNode = node.get("data");

            HashMap<String, String> models = new HashMap<>();

            dataNode.forEach(model -> {
                String id = model.get("id").asText();
                String modelName = model.get("name").asText();

                models.put(modelName, id);
            });

            String selectedModel = event.getFocusedOption().getValue().toLowerCase();
            AtomicInteger count = new AtomicInteger();

            event.replyChoices(models.keySet()
                    .stream()
                    .filter(modelName -> modelName.toLowerCase().contains(selectedModel)
                            && count.getAndIncrement() < 25)
                    .map(modelName -> new Command.Choice(modelName, models.get(modelName))).toList()).queue();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
