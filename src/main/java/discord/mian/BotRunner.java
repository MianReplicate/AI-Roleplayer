package discord.mian;

import discord.mian.ai.AIBot;
import discord.mian.custom.Cats;
import discord.mian.custom.Constants;
import discord.mian.custom.Util;
import io.github.freya022.botcommands.api.core.JDAService;
import io.github.freya022.botcommands.api.core.events.BReadyEvent;
import io.github.freya022.botcommands.api.core.service.annotations.BService;
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

@BService
public class BotRunner extends JDAService {
    @Override
    public @NotNull Set<GatewayIntent> getIntents() {
        return GatewayIntent.getIntents(GatewayIntent.ALL_INTENTS);
    }

    @Override
    public @NotNull Set<CacheFlag> getCacheFlags() {
        return Set.of();
    }

    @Override
    protected void createJDA(@NotNull BReadyEvent bReadyEvent, @NotNull IEventManager iEventManager) {
        Constants.LOGGER.info("IT'S TIME TO ROLEPLAY KIDDOS");

        File serverDatas = Util.createFileRelativeToData("servers");
        if(!serverDatas.exists())
            serverDatas.mkdir();

        try{
            new AIBot(create(Main.DISCORD_TOKEN)
                    .setEventManager(new AnnotatedEventManager())
                    .addEventListeners(new Listener())
                    .build());
        }catch(Exception e){
            Constants.LOGGER.error("Failure during initialization", e);
//            throw e;
        }

        Cats.create();
        try (ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor()) {
            Constants.LOGGER.info("Set up randomizing cat every hour..");
            scheduler.scheduleAtFixedRate(Cats::create, 0, 1, TimeUnit.HOURS);
        }
    }
}
