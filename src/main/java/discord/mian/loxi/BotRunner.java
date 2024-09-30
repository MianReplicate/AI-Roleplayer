package discord.mian.loxi;

import discord.mian.loxi.common.Bot;
import discord.mian.loxi.common.Listener;
import discord.mian.loxi.common.util.Constants;
import io.github.sashirestela.openai.SimpleOpenAI;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.hooks.AnnotatedEventManager;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.requests.restaction.CommandListUpdateAction;

// TODO: Remake all features from the original ai typescript branch into java branch. I LOVE JAVA :D
public class BotRunner {
    public static Bot bot;

    public static void main(String[] args) {
        Constants.LOGGER.info("IT'S TIME TO ROLEPLAY KIDDOS");
        bot = new Bot(JDABuilder.create(Constants.BOT_TOKEN, GatewayIntent.getIntents(GatewayIntent.ALL_INTENTS))
                .setEventManager(new AnnotatedEventManager())
                .addEventListeners(new Listener())
                .build());
    }
}
