package discord.mian.commands.custom;

import discord.mian.commands.SlashCommand;
import discord.mian.interactions.Interactions;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.InteractionContextType;

public class Help extends SlashCommand {
    public Help(){
        super("help", "Get some help for the roleplayer!");
        this.setContexts(InteractionContextType.GUILD);
    }

    @Override
    public boolean handle(SlashCommandInteractionEvent event) throws Exception {
        if(super.handle(event)) {
            event.replyComponents(Interactions.getHelpContainer())
                    .useComponentsV2()
                    .setEphemeral(true)
                    .queue();
            return true;
        }
        return false;
    }
}
