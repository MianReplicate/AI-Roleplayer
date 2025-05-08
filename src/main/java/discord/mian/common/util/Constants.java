package discord.mian.common.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class Constants {
    public static final Logger LOGGER = LoggerFactory.getLogger("Loxi-AI");
//    public static final String GROQ_TOKEN = System.getenv("GROQ_TOKEN");
//    public static final String SPOTIFY_TOKEN
//    public static final String GROQ_BASE_API = "https://api.groq.com/openai";
//    public static final String LLAMA_MODEL = "llama3-70b-8192";
public static final String LLAMA_MODEL = "meta-llama/llama-3.1-8b-instruct:free";
//    public static final String COSMO_MODEL = "cosmosrp";
//    public static final String COSMO_BASE_API = "https://api.pawan.krd/cosmosrp";
    public static final List<String> ALLOWED_USER_IDS = List.of(
            "546194587920760853"
    );
    public static final List<String> ALLOWED_SERVERS = List.of(
            "1269814553353519115"
    );
    public static final boolean PUBLIC = true;
}
