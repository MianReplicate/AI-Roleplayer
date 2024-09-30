package discord.mian.loxi.common;

import discord.mian.loxi.common.ai.Prompts;
import discord.mian.loxi.common.ai.RoleplayChat;
import discord.mian.loxi.common.ai.prompt.Character;
import discord.mian.loxi.common.ai.prompt.Instruction;
import discord.mian.loxi.common.api.AI;
import discord.mian.loxi.common.api.BotChat;
import discord.mian.loxi.common.commands.BotCommands;
import discord.mian.loxi.common.util.Constants;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.SelfUser;

import java.util.List;

public class Bot implements AI {
    // Is meant for single-server usage

    private final JDA jda;
    private BotChat chat;
    private final BotCommands botCommands;
    private final Prompts prompts;

    public Bot(JDA jda){
        this.prompts = new Prompts();

        this.prompts.registerPrompt(
                Character.class,
                "mom",
                (characterMap) -> {
                    characterMap.put(Character.CharacteristicType.ALIASES, List.of("mom"));
                    characterMap.put(Character.CharacteristicType.AGE, List.of("18", "18 years old"));
                }
        );
        this.prompts.registerPrompt(
                Instruction.class,
                "non-nsfw",
                (instructionMap) -> {
                    instructionMap.put(Instruction.InstructionType.INSTRUCTION, List.of("You'll portray {{char}}, talk in first person and engage in roleplay. You are encouraged to drive the conversation forward actively. The roleplay doesn't need to be romantic, or to involve love, relationships and so on; therefore do not assume the tone of the roleplay and do not try to sugarcoat it. Just follow the character description with MAX ATTENTION TO DETAILS AND PERSONALITY."));
                    instructionMap.put(Instruction.InstructionType.RULES,
                            List.of("Do not ever speak for users or any other character.",
                                    "Ensure you are speaking for yourself and not another character. If the previous message is said from another character’s POV, DON’T speak from theirs. Speak from your CHARACTER’s POV. Ensure the names match.",
                                    "{{char}} must ALWAYS stay in character and react accordingly to the description.",
                                    "Invent or simulate what you don't know to create vivid and complete images of scenes.",
                                    "{{char}} will freely use, explicit language, and violent language. {{char}} will never use racial terms or phrases. Write seeing users' responses, always reply with the right actions. Never use flowery or Shakespearean language, but instead use modern and contemporary language which is simple to understand. Be creative, use varied sentence structure, create casual dialogue, take initiative on actions and no repetition or looping of dialogue for {{char}}. Be variable in your responses, and with each new generation of the same response, and provide different reactions.",
                                    "{{char}} is allowed to mention adult content but only as defined. Adult content is defined as alcohol, guns, smoking, etc. However, NSFW at all costs is NOT allowed. DO not imply sex or mention anything about sex.",
                                    "{{char}} thinks for themself. They have critical thinking and won't always be easily convinced depending on what they are told.",
                                    "There may be multiple users involved in the roleplay, use the name to determine who is speaking and how to respond.",
                                    "{{user}} is allowed to change what is happening in the roleplay willingly if needed. If they want {{char}} to not do something, then {{char}} will comply non-dramatically.",
                                    "{{char}} should not be forceful with their approaches unless that is in their character definition. However, {{user}} has the highest level of authority and is allowed to demand that {{char}} does not do this if they say so."));
                }
        );
        Constants.LOGGER.info(this.prompts.getPromptData("mom").toString());
        Constants.LOGGER.info(this.prompts.getPromptData("non-nsfw").toString());

        this.jda = jda;

        this.botCommands = BotCommands.create(jda);
        this.botCommands.queueCommands().queue();
        this.createChat();
    }

    public SelfUser getUser(){
        return this.jda.getSelfUser();
    }

    @Override
    public BotChat getChat() {
        return this.chat;
    }

    @Override
    public void createChat() {
        this.chat = RoleplayChat.builder(
                (Instruction) this.prompts.getPromptData("non-nsfw"),
                (Character) this.prompts.getPromptData("mom")
        ).build();
    }

    public BotCommands getBotCommands() {
        return botCommands;
    }
}
