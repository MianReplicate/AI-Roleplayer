package discord.mian.components;

import discord.mian.components.custom.DirectionSwap;
import discord.mian.components.custom.NewRoleplay;

import java.util.List;

public class Components {
    public static final List<Component> components = List.of(
            new NewRoleplay(),
            new DirectionSwap("back"),
            new DirectionSwap("next")
    );
}
