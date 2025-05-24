package discord.mian.commands.custom;

import discord.mian.commands.SlashCommand;
import discord.mian.Constants;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.managers.AccountManager;

public class ChangeAvatar extends SlashCommand {
    public ChangeAvatar() {
        super("change_avatar", "Change my pictures :o, so smexy");
        this.permissionHandler.addUsers(Constants.ALLOWED_USER_IDS);
        this.addOption(OptionType.ATTACHMENT, "avatar", "My new avatar :D", false);
        this.addOption(OptionType.ATTACHMENT, "banner", "BANNER!", false);
    }

    @Override
    public boolean handle(SlashCommandInteractionEvent event) throws Exception {
        if (super.handle(event)) {
            event.deferReply(true).queue();
            Message.Attachment avatar = event.getOption("avatar", OptionMapping::getAsAttachment);
            Message.Attachment banner = event.getOption("banner", OptionMapping::getAsAttachment);
            AccountManager manager = event.getJDA().getSelfUser().getManager();

            if (avatar != null)
                manager.setAvatar(avatar.getProxy().downloadAsIcon().get()).queue();
            if (banner != null)
                manager.setBanner(banner.getProxy().downloadAsIcon().get()).queue();

            event.getHook().editOriginal("Successfully changed my images :O").queue();
            return true;
        }
        return false;
    }
}
