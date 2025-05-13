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

public class Main {
    public static String DISCORD_TOKEN;
    public static void main(String[] args) throws Exception {
        DISCORD_TOKEN = args[0];

        BotCommands.create(bConfigBuilder -> {

            bConfigBuilder.applicationCommands(appCommands ->
                    appCommands.enable(true));
            bConfigBuilder.modals(modals -> modals.enable(true));
            bConfigBuilder.components(components -> components.enable(true));
            bConfigBuilder.addSearchPath("discord.mian");
        });
    }
}
