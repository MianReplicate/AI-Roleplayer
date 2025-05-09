package discord.mian.common.commands;

import discord.mian.common.AIBot;
import discord.mian.common.commands.api.CommandHandler;
import discord.mian.common.commands.custom.*;
import discord.mian.common.util.Constants;
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.GenericCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.requests.restaction.CommandListUpdateAction;

import java.util.List;
import java.util.Optional;

public class BotCommands {
    private static final List<CommandHandler> commands = List.of(
            new NewChat(),
            new GetPrompt(),
            new ChangeModel(),
            new Talk(),
            new Menu()
    );

    public static CommandListUpdateAction addCommands(){
        Constants.LOGGER.info("Adding bot commands!");

        return AIBot.bot.getJDA().updateCommands().addCommands(commands);
    }

    public static void handleCommand(GenericCommandInteractionEvent event) throws Exception {
        Optional<CommandHandler> commandOptional = commands.stream().filter(command -> command.getName().equalsIgnoreCase(event.getName()))
                .findFirst();
        if(commandOptional.isPresent()){
            CommandHandler<GenericCommandInteractionEvent> commandHandler = commandOptional.get();
            try{
                commandHandler.handle(event);
            } catch (Exception e) {
                event.reply("Failed to execute command :<").setEphemeral(true).queue();
                throw(e);
            }
        }
    }

    public static void handleAutoComplete(CommandAutoCompleteInteractionEvent event) {
        commands.stream().filter(command -> command.getName().equalsIgnoreCase(event.getName()) && command instanceof SlashCommand)
                .findFirst()
                .ifPresent(command -> {
                    try {
                        SlashCommand slashCommand = (SlashCommand) command;
                        slashCommand.autoComplete(event);
                    } catch (Exception ignored) {

                    }
                });
    }
}
