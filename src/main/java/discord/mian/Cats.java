package discord.mian;

import okhttp3.Call;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import java.io.InputStream;
import java.util.Random;

public class Cats {
    private static byte[] file;
    private static boolean IsGif = false;

    public static void create() {
        Constants.LOGGER.info("Getting new cat file!");

        Random random = new Random();
        boolean isGif = random.nextBoolean();
        String url = "https://cataas.com/cat";
        if (isGif)
            url = "https://cataas.com/cat/gif";

        OkHttpClient client = new OkHttpClient.Builder().build();

        Request request = new Request.Builder()
                .url(url)
                .get()
                .build();

        Call call = client.newCall(request);
        try(Response response = call.execute()){
            InputStream inputStream = response.body().byteStream();
            file = inputStream.readAllBytes();
            IsGif = isGif;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static byte[] getCat() {
        return file;
    }

    public static boolean isGif() {
        return IsGif;
    }
}
