package discord.mian.loxi.common.commands;

import discord.mian.loxi.common.commands.custom.GetPrompt;
import discord.mian.loxi.common.commands.custom.NewChat;
import discord.mian.loxi.common.commands.custom.RequeueCommands;
import discord.mian.loxi.common.commands.custom.RestartHistory;
import discord.mian.loxi.common.util.Constants;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.requests.restaction.CommandListUpdateAction;

import java.util.ArrayList;
import java.util.List;

public class BotCommands {
    private List<AbstractCommand> commands = List.of();
    private JDA jda;
    // Can be ran multiple times in order to reregister new commands
    private BotCommands(JDA jda){
        this.jda = jda;
    }

    public static BotCommands create(JDA jda){
        return new BotCommands(jda);
    }

    public CommandListUpdateAction addCommands(){
        Constants.LOGGER.info("Adding bot commands!");
        this.commands = List.of(
                new NewChat(),
                new RestartHistory(),
                new RequeueCommands(),
                new GetPrompt()
        );

        CommandListUpdateAction updateCommands = this.jda.updateCommands();
        ArrayList<CommandData> commandDatas = new ArrayList<>();
        this.commands.forEach(command ->
                commandDatas.add(Commands.slash(command.getName(), command.getDescription()).addOptions(command.getOptionDatas())));
        return updateCommands.addCommands(commandDatas);
    }

    public List<AbstractCommand> getCommands(){
        return this.commands;
    }
}
