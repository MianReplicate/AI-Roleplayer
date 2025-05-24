package discord.mian.commands.custom;

import discord.mian.commands.SlashCommand;
import discord.mian.Constants;
import discord.mian.interactions.Interactions;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.InteractionContextType;

import java.io.IOException;

public class Menu extends SlashCommand {


    public Menu() {
        super("menu", "Open the AI menu");
        this.setContexts(InteractionContextType.GUILD);
    }

    @Override
    public boolean handle(SlashCommandInteractionEvent event) throws Exception {
        if (super.handle(event)) {
            event.deferReply().setEphemeral(true).useComponentsV2().queue();
            event.getHook().retrieveOriginal().queue(msg -> {
                try {
                    Interactions.createDashboard(msg);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }, t -> {
                event.getHook().editOriginal("Failed to get dashboard!").queue();
                Constants.LOGGER.error("Dashboard failed to open", t);
            });
            return true;
        }
        return false;
    }
}
