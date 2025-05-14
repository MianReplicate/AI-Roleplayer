package discord.mian.commands.custom;

import discord.mian.commands.SlashCommand;
import discord.mian.interactions.Interactions;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;

public class Menu extends SlashCommand {


    public Menu(){
        super("menu", "Open the AI menu");
    }

    @Override
    public boolean handle(SlashCommandInteractionEvent event) throws Exception {
        if(super.handle(event)){
            event.deferReply().queue();
            Interactions.createDashboard(event.getHook().retrieveOriginal().submit().get());
            return true;
        }
        return false;
    }
}
