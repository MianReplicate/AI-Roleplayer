package discord.mian;

import discord.mian.ai.AIBot;
import discord.mian.custom.Cats;
import discord.mian.custom.Constants;
import discord.mian.custom.Util;
import io.github.freya022.botcommands.api.core.BotCommands;
import io.github.freya022.botcommands.api.core.events.BReadyEvent;
import net.dv8tion.jda.api.hooks.AnnotatedEventManager;
import net.dv8tion.jda.api.hooks.IEventManager;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.cache.CacheFlag;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

//TODO:
// automatic character talking?
// rework new roleplay button to allow you to add characters and instructions via prompt options :D
// Allow the use of multiple instructions (allow putting them in a specific order)
// ability to set keys for each server
// maybe ability to save chat history i dunno
// set up permissions
// chat history should change slightly, it should include every msg beginnign from start of roleplay
// allow for retrying a broken response (remove broken response if a new respoen gets generated)

public class Main {
    public static String DISCORD_TOKEN;
    public static void main(String[] args) throws Exception {
        DISCORD_TOKEN = args[0];

        BotCommands.create(bConfigBuilder -> {

            bConfigBuilder.applicationCommands(bApplicationConfigBuilder ->
                    bApplicationConfigBuilder.enable(true));
            bConfigBuilder.components(components -> components.enable(true));
            bConfigBuilder.addSearchPath("discord.mian");
//            bConfigBuilder.add();
        });

//
//        Constants.LOGGER.info("IT'S TIME TO ROLEPLAY KIDDOS");
//
//        File serverDatas = Util.createFileRelativeToData("servers");
//        if(!serverDatas.exists())
//            serverDatas.mkdir();
//
//        try{
//            new AIBot(JDABuilder.create(discord_bot_token, GatewayIntent.getIntents(GatewayIntent.ALL_INTENTS))
//                    .setEventManager(new AnnotatedEventManager())
//                    .addEventListeners(new Listener())
//                    .build());
//        }catch(Exception e){
//            Constants.LOGGER.error("Failure during initialization", e);
//            throw e;
//        }
//
//        Cats.create();
//        try (ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor()) {
//            Constants.LOGGER.info("Set up randomizing cat every hour..");
//            scheduler.scheduleAtFixedRate(Cats::create, 0, 1, TimeUnit.HOURS);
//        }
    }
}
