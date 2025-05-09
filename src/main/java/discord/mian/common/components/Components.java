package discord.mian.common.components;

import discord.mian.common.components.custom.DirectionSwap;
import discord.mian.common.components.custom.RestartHistory;

import java.util.List;

public class Components {
    public static final List<Component> components = List.of(
            new RestartHistory(),
            new DirectionSwap("back"),
            new DirectionSwap("next")
    );
}
