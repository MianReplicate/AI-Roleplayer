package discord.mian.components.custom;

import discord.mian.ai.AIBot;
import discord.mian.components.Component;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutionException;

public class NewRoleplay extends Component<ButtonInteractionEvent> {
    public NewRoleplay(){
        super("new_roleplay");
    }

    @Override
    public boolean handle(ButtonInteractionEvent event) throws Exception {
        if(super.handle(event)){
            event.reply("Started new chat! From here on out, characters will be listening :o").queue(success -> {
                try {
                    AIBot.bot.getChat(event.getGuild())
                            .startRoleplay(success.retrieveOriginal().submit().get(),
                                    AIBot.bot.getServerData(event.getGuild()).getInstructionDatas().get("non-nsfw"), List.of());
                } catch (ExecutionException | InterruptedException | IOException e) {
                    event.getHook().editOriginal("Failed to start chat!").queue();
                }
            });

            return true;
        }
        return false;
    }
}
