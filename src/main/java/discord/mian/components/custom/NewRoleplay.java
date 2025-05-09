package discord.mian.components.custom;

import discord.mian.AIBot;
import discord.mian.components.Component;
import discord.mian.ai.prompt.Instruction;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;

import java.util.List;
import java.util.concurrent.ExecutionException;

public class NewRoleplay extends Component<ButtonInteractionEvent> {
    public NewRoleplay(){
        super("new_roleplay");
    }

    @Override
    public boolean handle(ButtonInteractionEvent event) throws Exception {
        if(super.handle(event)){
            try {
                AIBot.bot.getChat(event.getGuild())
                        .startRoleplay(event.getGuildChannel().asTextChannel(),
                                (Instruction) AIBot.bot.getPrompts().getPromptData("non-nsfw"), List.of());
                event.reply("Started new chat!").queue();
            } catch (ExecutionException | InterruptedException e) {
                event.reply("Failed to start chat!").queue();
                throw new RuntimeException(e);
            }

            return true;
        }
        return false;
    }
}
