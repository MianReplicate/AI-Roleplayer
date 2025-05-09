package discord.mian.components.custom;

import discord.mian.AIBot;
import discord.mian.ai.DiscordRoleplay;
import discord.mian.components.Component;
import discord.mian.custom.Direction;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;

public class DirectionSwap extends Component<ButtonInteractionEvent> {
    public DirectionSwap(String direction){
        super(direction);
    }

    @Override
    public boolean handle(ButtonInteractionEvent event) throws Exception {
        if(super.handle(event)){
            event.deferReply().setEphemeral(true).queue();

            DiscordRoleplay roleplay = AIBot.bot.getChat(event.getGuild());
            if(roleplay.isRunningRoleplay()){
                roleplay.swipe(event, id.equals("back") ? Direction.BACK : Direction.NEXT);
            } else {
                event.getHook().editOriginal("Roleplay isn't running currently!").queue();
            }

            return true;
        }
        return false;
    }
}
