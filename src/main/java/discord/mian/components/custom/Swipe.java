package discord.mian.components.custom;

import discord.mian.ai.AIBot;
import discord.mian.ai.DiscordRoleplay;
import discord.mian.components.Component;
import discord.mian.custom.Direction;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;

public class Swipe extends Component<ButtonInteractionEvent> {
    public Swipe(String direction){
        super(direction+"_swipe");
    }

    @Override
    public boolean handle(ButtonInteractionEvent event) throws Exception {
        if(super.handle(event)){
            event.deferEdit().queue();

            DiscordRoleplay roleplay = AIBot.bot.getChat(event.getGuild());
            if(roleplay.isRunningRoleplay()){
                roleplay.swipe(event, id.equals("back_swipe") ? Direction.BACK : Direction.NEXT);
            } else {
                event.getHook().editOriginal("Roleplay isn't running currently!").queue();
            }

            return true;
        }
        return false;
    }
}
