package discord.mian.commands;

import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.GenericInteractionCreateEvent;
import net.dv8tion.jda.api.interactions.callbacks.IReplyCallback;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

public class PermissionHandler<T extends GenericInteractionCreateEvent & IReplyCallback> {
    private final ArrayList<Long> allowedUsers = new ArrayList<>();

    public PermissionHandler<T> addUsers(Long... users) {
        return addUsers(Arrays.stream(users).toList());
    }

    public PermissionHandler<T> addUsers(Collection<Long> users) {
        allowedUsers.addAll(users);
        return this;
    }

    public boolean isAllowed(User user) {
        // if empty then it is assumed that everyone is allowed
        return allowedUsers.isEmpty() || allowedUsers.contains(user.getIdLong());
    }

    public boolean handle(T event) throws Exception {
        if (isAllowed(event.getUser()))
            return true;
        else
            event.reply("This command is restricted to a set user list. If you believe this is a mistake, please contact the owner.").setEphemeral(true).queue();
        return false;
    }
}
