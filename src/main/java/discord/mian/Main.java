package discord.mian;

import discord.mian.ai.AIBot;
import discord.mian.custom.Cats;
import discord.mian.custom.Constants;
import discord.mian.custom.Util;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.hooks.AnnotatedEventManager;
import net.dv8tion.jda.api.requests.GatewayIntent;

import java.io.File;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class Main {
    public static void main(String[] args) throws Exception {
        String discord_bot_token = args[0];

        Constants.LOGGER.info("IT'S TIME TO ROLEPLAY KIDDOS");

        File serverDatas = Util.createFileRelativeToData("servers");
        if (!serverDatas.exists())
            serverDatas.mkdir();

        try {
            new AIBot(JDABuilder.create(discord_bot_token, GatewayIntent.getIntents(GatewayIntent.ALL_INTENTS))
                    .setEventManager(new AnnotatedEventManager())
                    .addEventListeners(new Listener())
                    .build());
        } catch (Exception e) {
            Constants.LOGGER.error("Failure during initialization", e);
            throw e;
        }

        Cats.create();
        try (ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor()) {
            Constants.LOGGER.info("Set up randomizing cat every hour..");
            scheduler.scheduleAtFixedRate(Cats::create, 0, 1, TimeUnit.HOURS);
        }
    }
}
