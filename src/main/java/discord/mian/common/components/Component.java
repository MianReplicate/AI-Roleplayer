package discord.mian.common.components;

import discord.mian.common.PermissionHandler;
import net.dv8tion.jda.api.events.interaction.component.GenericComponentInteractionCreateEvent;

public abstract class Component<T extends GenericComponentInteractionCreateEvent> {
    protected final PermissionHandler<T> permissionHandler;
    public final String id;

    protected Component(String id){
        this.id = id;
        this.permissionHandler = new PermissionHandler<>();
    }

    public boolean handle(T event) throws Exception {
        return permissionHandler.handle(event);
    }
}
