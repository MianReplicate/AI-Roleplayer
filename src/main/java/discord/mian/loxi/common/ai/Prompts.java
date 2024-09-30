package discord.mian.loxi.common.ai;

import discord.mian.loxi.common.ai.prompt.Prompt;
import discord.mian.loxi.common.api.Type;
import discord.mian.loxi.common.util.Constants;

import java.util.*;
import java.util.function.Consumer;

public class Prompts {
    private Map<String, Prompt> promptMap = new HashMap<>();
    public <T extends Type> void registerPrompt(Class clazz, String promptName, Consumer<Map<T, List<String>>> consumer) {
        if(!Prompt.class.isAssignableFrom(clazz))
            throw new RuntimeException(clazz + " is not an instance of "+Prompt.class);
        Objects.requireNonNull(consumer);
        LinkedHashMap<T, List<String>> typeMap = new LinkedHashMap<>();
        consumer.accept(typeMap);
        Prompt<T> prompt = null;
        try{
            prompt = (Prompt<T>) Arrays.stream(clazz.getConstructors()).findAny().get().newInstance(typeMap);
        } catch (Exception e) {}
        if(prompt != null)
            promptMap.put(promptName, prompt);
        else
            Constants.LOGGER.info("Prompt is null");
    }
    public Prompt getPromptData(String name){
        return this.promptMap.get(name);
    }
}
