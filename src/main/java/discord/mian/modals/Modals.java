package discord.mian.modals;

import discord.mian.modals.custom.EditMsg;
import discord.mian.modals.custom.PromptEditor;

import java.util.List;

public class Modals {
    public static final List<Modal> modals = List.of(
            new EditMsg(),
            new PromptEditor()
    );
}
