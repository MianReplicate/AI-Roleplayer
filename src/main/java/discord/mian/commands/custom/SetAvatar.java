package discord.mian.commands.custom;

import com.fasterxml.jackson.databind.ObjectMapper;
import discord.mian.ai.AIBot;
import discord.mian.commands.SlashCommand;
import discord.mian.custom.Util;
import discord.mian.data.CharacterData;
import discord.mian.data.Server;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.InteractionContextType;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class SetAvatar extends SlashCommand {

    public SetAvatar() {
        super("set_avatar", "Assign an avatar to a character!");
        this.addOptions(
                new OptionData(OptionType.STRING, "name", "The name of the character", true, true),
                new OptionData(OptionType.ATTACHMENT, "avatar", "An avatar for the character. Should be a PNG or a JPG", true)
        );
        this.setContexts(InteractionContextType.GUILD);
    }

    @Override
    public boolean handle(SlashCommandInteractionEvent event) throws Exception {
        if (super.handle(event)) {
            if (!Util.hasMasterPermission(event.getMember())) {
                event.reply("nuh uh little bro bro, you dont got permission").setEphemeral(true).queue();
                return true;
            }

            event.deferReply().setEphemeral(true).queue();
            Server server = AIBot.bot.getServerData(event.getGuild());
            String name = event.getOption("name", OptionMapping::getAsString);
            CharacterData existingCharacter = server.getCharacterDatas().get(name);

            Message.Attachment avatar = event.getOption("avatar", OptionMapping::getAsAttachment);

            if (existingCharacter == null) {
                event.getHook().editOriginal("There is no existing character with this name!").queue();
                return true;
            }

            if (!validateImage(avatar)) {
                event.getHook().editOriginal("The image provided was invalid! Make sure it is not corrupted and has a valid extension!").queue();
                return true;
            }

            File existingAvatar = existingCharacter.getAvatar();
            if (existingAvatar != null)
                existingAvatar.delete();

            File avatarFile = new File(existingCharacter.getCharacterFolder().getPath() + "/avatar." + avatar.getFileExtension());
            avatarFile.createNewFile();
            avatar.getProxy().downloadToFile(avatarFile).get();

            ObjectMapper mapper = new ObjectMapper();
            File dataJson = new File(existingCharacter.getCharacterFolder().getPath() + "/data.json");
            if (dataJson.exists()) {
                Map<String, String> map = mapper.readValue(dataJson, Map.class);
                map.put("avatar_link", null);
                mapper.writeValue(dataJson, map);
            }

            event.getHook().editOriginal("Successfully added or replaced avatar for " + name).queue();
            return true;
        }

        return false;
    }

    @Override
    public void autoComplete(CommandAutoCompleteInteractionEvent event) {
        List<String> characterNames = AIBot.bot.getServerData(event.getGuild()).getCharacterDatas().keySet().stream()
                .filter(string -> string.toLowerCase().startsWith(event.getFocusedOption().getValue().toLowerCase())).toList();
        if (characterNames.size() >= 25)
            characterNames = characterNames.subList(0, 25);
        List<Command.Choice> choices = new ArrayList<>();
        characterNames.forEach(name -> choices.add(new Command.Choice(name, name)));

        event.replyChoices(choices).queue();
    }

    public boolean validateImage(Message.Attachment image) {
        if (image == null)
            return false;

        return image.isImage() && Util.IMAGE_EXTENSIONS.contains("." + image.getFileExtension());
    }
}
