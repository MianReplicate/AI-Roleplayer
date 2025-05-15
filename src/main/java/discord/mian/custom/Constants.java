package discord.mian.custom;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class Constants {
    public static final Logger LOGGER = LoggerFactory.getLogger("AI Roleplayer");
    public static final String BASE_URL = "https://openrouter.ai/api";
    public static final String DEFAULT_MODEL = "meta-llama/llama-3.3-70b-instruct:free";

    public static final List<String> ALLOWED_USER_IDS = List.of(
            "546194587920760853"
    );
    public static final List<String> ALLOWED_SERVERS = List.of(
            "1269814553353519115"
    );
    public static final boolean PUBLIC = true;
}
