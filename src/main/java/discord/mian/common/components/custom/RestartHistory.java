package discord.mian.common.components.custom;

import discord.mian.common.AIBot;
import discord.mian.common.components.Component;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyCallbackAction;

import java.util.function.Consumer;

public class RestartHistory extends Component<ButtonInteractionEvent> {
    public RestartHistory(){
        super("restart_history");
    }

    @Override
    public boolean handle(ButtonInteractionEvent event) throws Exception {
        if(super.handle(event)){
            Consumer<InteractionHook> consumer = (interactionHook) -> AIBot.bot.getChat(event.getGuild()).restartHistory();
            ReplyCallbackAction reply = event.reply("Restarting history!");
            reply.queue(consumer);

            return true;
        }
        return false;
    }
}
