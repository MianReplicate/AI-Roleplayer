package discord.mian.components;

import discord.mian.components.custom.*;

import java.util.List;

public class Components {
    public static final List<Component> components = List.of(
            new NewRoleplay(),
            new Swipe("back"),
            new Swipe("next"),
            new PromptSwipe("back"),
            new PromptSwipe("next"),
            new Destroy(),
            new EditMessage(),
            new ViewInstalledDatas("characters"),
            new ViewInstalledDatas("instructions"),
            new ViewMenu(),
            new EditPrompt(),
            new CreatePrompt(),
            new DeletePrompt()
    );
}
