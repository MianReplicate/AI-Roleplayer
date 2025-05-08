package discord.mian.common;

import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.GenericInteractionCreateEvent;
import net.dv8tion.jda.api.interactions.callbacks.IReplyCallback;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

public class PermissionHandler<T extends GenericInteractionCreateEvent & IReplyCallback> {
    private final ArrayList<User> allowedUsers = new ArrayList<>();

    public PermissionHandler<T> addUsers(User... users){
        return addUsers(Arrays.stream(users).toList());
    }

    public PermissionHandler<T> addUsers(Collection<User> users){
        allowedUsers.addAll(users);
        return this;
    }

    public boolean isAllowed(User user){
        // if empty then it is assumed that everyone is allowed
        return allowedUsers.isEmpty() || allowedUsers.contains(user);
    }

    public boolean handle(T event) throws Exception {
        if(isAllowed(event.getUser()))
            return true;
        else
            event.reply("You are not allowed to use this command!").setEphemeral(true).queue();
        return false;
    }
}
