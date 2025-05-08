package discord.mian.common;

import discord.mian.common.util.Constants;

import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Random;

public class Cat {
    private static byte[] file;
    private static boolean IsGif = false;

    public static void create(){
        Constants.LOGGER.info("Getting new cat file!");

        Random random = new Random();
        boolean isGif = random.nextBoolean();
        String url = "https://cataas.com/cat";
        if(isGif)
            url = "https://cataas.com/cat/gif";

        HttpClient httpClient = HttpClient.newHttpClient();

        HttpRequest request = HttpRequest
                .newBuilder(URI.create(url))
                .GET()
                .build();

        try {
            HttpResponse<InputStream> response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());
            InputStream inputStream = response.body();
            file = inputStream.readAllBytes();
            IsGif = isGif;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static byte[] getCat(){
        return file;
    }

    public static boolean isGif() {
        return IsGif;
    }
}
