package discord.mian;

import discord.mian.common.AIBot;
import discord.mian.common.Listener;
import discord.mian.common.util.Constants;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.hooks.AnnotatedEventManager;
import net.dv8tion.jda.api.requests.GatewayIntent;

//TODO:
// AI chatting should be specific to each server
// Allow saving for AI chats
// Add system prompt
// Add option to add prompts via messages or use an interactive UI menu to add to each type

public class BotRunner {
    public static void main(String[] args) {
        String discord_bot_token = args[0];

        Constants.LOGGER.info("IT'S TIME TO ROLEPLAY KIDDOS");
        try{
            new AIBot(JDABuilder.create(discord_bot_token, GatewayIntent.getIntents(GatewayIntent.ALL_INTENTS))
                    .setEventManager(new AnnotatedEventManager())
                    .addEventListeners(new Listener())
                    .build());
        }catch(Throwable t){
            Constants.LOGGER.error("Failure during initialization", t);
            throw t;
        }
    }
}
