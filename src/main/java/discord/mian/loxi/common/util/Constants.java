package discord.mian.loxi.common.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class Constants {
    public static final Logger LOGGER = LoggerFactory.getLogger("Loxi-AI");
    public static final String BOT_TOKEN = System.getenv("DISCORD_BOT_TOKEN");
    public static final String GROQ_TOKEN = System.getenv("GROQ_API");
    public static final String GROQ_BASE_API = "https://api.groq.com/openai";
    public static final String LLAMA_MODEL = "llama3-70b-8192";
    public static final String COSMO_MODEL = "cosmosrp";
    public static final List<String> ALLOWED_USER_IDS = List.of(
            "546194587920760853"
    );
    public static final List<String> ALLOWED_SERVERS = List.of(
            "691875797945810976"
    );
}
