package discord.mian.components;

import discord.mian.components.custom.*;

import java.util.List;

public class Components {
    public static final List<Component> components = List.of(
            new NewRoleplay(),
            new DirectionSwap("back"),
            new DirectionSwap("next"),
            new Destroy(),
            new Edit(),
            new ViewInstalledDatas("characters"),
            new ViewInstalledDatas("introductions"),
            new ViewMenu()
    );
}
