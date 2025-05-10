package discord.mian.commands.custom;

import discord.mian.commands.SlashCommand;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;

public class AddCharacter extends SlashCommand {

    public AddCharacter() {
        super("addcharacter", "Add or edit a new character prompt to the bot!");
        this.addOptions(
                new OptionData(OptionType.STRING, "name", "The name of the character", true),
                new OptionData(OptionType.ATTACHMENT, "definition", "A text file containing the entire character's description", false),
                new OptionData(OptionType.ATTACHMENT, "avatar", "An avatar for the character. Should be a PNG or a JPG", false)
        );
    }

    @Override
    public boolean handle(SlashCommandInteractionEvent event) throws Exception {
        if(super.handle(event)){
            String name = event.getOption("name", OptionMapping::getAsString);
            Message.Attachment definition = event.getOption("definition", OptionMapping::getAsAttachment);
            Message.Attachment avatar = event.getOption("avatar", OptionMapping::getAsAttachment);


        }

        return false;
    }
}
