package discord.mian.commands;

import discord.mian.custom.Constants;
import io.github.freya022.botcommands.api.commands.application.ApplicationCommand;
import io.github.freya022.botcommands.api.core.BotCommands;
import net.dv8tion.jda.api.requests.restaction.CommandListUpdateAction;

import java.util.List;

public class Commands {
    private static final List<ApplicationCommand> commands = List.of(
//            new NewChat(),
//            new ChangeModel(),
//            new Talk(),
//            new Menu()
//            new AddAvatar()
    );

    public static void addCommands(){
        Constants.LOGGER.info("Adding bot commands!");

        BotCommands.create(bConfigBuilder -> {

//            bConfigBuilder.components(components -> components.enable(true));
            bConfigBuilder.addSearchPath("discord.mian");
//            bConfigBuilder.add();
        });
//        return AIBot.bot.getJDA().updateCommands().addCommands(commands);
    }

//    public static void handleCommand(GenericCommandInteractionEvent event) throws Exception {
//        Optional<CommandHandler> commandOptional = commands.stream().filter(command -> command.getName().equalsIgnoreCase(event.getName()))
//                .findFirst();
//        if(commandOptional.isPresent()){
//            CommandHandler<GenericCommandInteractionEvent> commandHandler = commandOptional.get();
//            try{
//                commandHandler.handle(event);
//            } catch (Exception e) {
//                event.getHook().retrieveOriginal().queue(
//                        message -> message.editMessage("Failed to execute command :<").queue(),
//                        failure -> event.reply("Failed to execute command :<").setEphemeral(true).queue()
//                );
//                throw(e);
//            }
//        }
//    }
//
//    public static void handleAutoComplete(CommandAutoCompleteInteractionEvent event) {
//        commands.stream().filter(command -> command.getName().equalsIgnoreCase(event.getName()) && command instanceof SlashCommand)
//                .findFirst()
//                .ifPresent(command -> {
//                    try {
//                        SlashCommand slashCommand = (SlashCommand) command;
//                        slashCommand.autoComplete(event);
//                    } catch (Exception ignored) {
//
//                    }
//                });
//    }
}
