package discord.mian;

import discord.mian.common.AIBot;
import discord.mian.common.Cats;
import discord.mian.common.Listener;
import discord.mian.common.util.Constants;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.hooks.AnnotatedEventManager;
import net.dv8tion.jda.api.requests.GatewayIntent;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

//TODO:
// AI chatting should be specific to each server
// Allow saving for AI chats, characters, yk, yk
// Add system prompt / introductions
// Add option to add prompts via messages or use an interactive UI menu to add to each type
// Add option to swipe on messages
// GROUP CAHTSSS
// (done) store messages as IDs, if users wanna delete bot/user messages, they can just delete it in discordd and it will reflect here auto
// use webhooks for roleplaying the characters
// character names will be given eventually not through prompts (cuz prompts will become custom soon)

public class BotRunner {
    public static void main(String[] args) throws InterruptedException {
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

        Cats.create();
        try (ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor()) {
            Constants.LOGGER.info("Set up randomizing cat every hour..");
            scheduler.scheduleAtFixedRate(Cats::create, 0, 1, TimeUnit.HOURS);
        }
    }
}
