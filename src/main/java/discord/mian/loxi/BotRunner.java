package discord.mian.loxi;

import discord.mian.loxi.common.Bot;
import discord.mian.loxi.common.Listener;
import discord.mian.loxi.common.util.Constants;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.hooks.AnnotatedEventManager;
import net.dv8tion.jda.api.requests.GatewayIntent;

//TODO:
// Allow bot to play music from spotify & audio from YT
// Add cmd to allow for role creating in a server which is assigned to users. Users can assign color and choose name
// AI chatting should be specific to each server
// Allow saving for AI chats
// Add system prompt
// Add option to add prompts via messages or use an interactive UI menu to add to each type

public class BotRunner {
    public static Bot bot;

    public static void main(String[] args) {
        Constants.LOGGER.info("IT'S TIME TO ROLEPLAY KIDDOS");
        try{
            bot = new Bot(JDABuilder.create(Constants.DISCORD_BOT_TOKEN, GatewayIntent.getIntents(GatewayIntent.ALL_INTENTS))
                    .setEventManager(new AnnotatedEventManager())
                    .addEventListeners(new Listener())
                    .build());
        }catch(Throwable t){
            Constants.LOGGER.error("Failure during initialization", t);
            throw t;
        }
    }
}
