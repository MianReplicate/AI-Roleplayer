package discord.mian.custom;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.*;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

public class Util {
    public static String botifyMessage(String string){
        return "```" + string + "```";
    }
    public static Connection openDatabase(String dbName){
        Connection c = null;

        try {
            Class.forName("org.sqlite.JDBC");
            c = DriverManager.getConnection("jdbc:sqlite:"+dbName+".db");
        } catch ( Exception e ) {
            System.err.println( e.getClass().getName() + ": " + e.getMessage() );
            System.exit(0);
        }
        System.out.println("Opened database successfully");

        return c;
    }

    public static File getDataFolder(){
        File data = new File("data");
        if(!data.exists())
            data.mkdir();
        return data;
    }

    public static File createFileRelativeToData(String name){
        return new File(getDataFolder().getName() + "/"+name);
    }

    public static byte[] getRandomImage() throws IOException {
        File images = createFileRelativeToData("images");
        if(!images.exists())
            images.mkdir();

        File[] files = images.listFiles();
        if(files.length == 0)
            return null;

        File[] randomImages = files[(int) (Math.random() * files.length)].listFiles();
        File randomImage = randomImages[(int) (Math.random() * randomImages.length)];
        if(!randomImage.exists())
            return null;

        return Files.readAllBytes(randomImage.toPath());
    }

    public static boolean isValidImage(File image){
        List<String> extensions = List.of(".jpg", ".jpeg", ".png", ".gif", ".webp");

        if(!image.exists())
            return false;

        String name = image.getName();
        String extension = name.substring(name.indexOf("."));

        return extensions.contains(extension);
    }

    public static String uploadImageToImgur(File image) throws IOException, InterruptedException {
        if(!isValidImage(image))
            throw new RuntimeException("Invalid image!");

        Constants.LOGGER.info("Attempting to upload " + image.getName());

        byte[] imageBytes = Files.readAllBytes(image.toPath());
//        String base64Image = Base64.getEncoder().encodeToString(imageBytes).replaceAll("\n", "");
//
//        HttpClient httpClient = HttpClient.newHttpClient();
//
//        HttpRequest request = HttpRequest
//                .newBuilder(URI.create("https://api.imgbb.com/1/upload"))
//                .header("Content-Type", "multipart/form-data")
//                .POST(
//                        HttpRequest.BodyPublishers.ofString("key="+Constants.IMGBB_TOKEN+"&image="+imageBytes)
//                )
//                .build();

        // this looks ugly i might replace it in the future but for now i could care less

        String boundary = UUID.randomUUID().toString();

        // Construct the multipart form-data body
        String body = "--" + boundary + "\r\n" +
                "Content-Disposition: form-data; name=\"key\"\r\n\r\n" +
                Constants.IMGBB_TOKEN + "\r\n" +  // Your API key here
                "--" + boundary + "\r\n" +
                "Content-Disposition: form-data; name=\"image\"; filename=\"" + image.getName() + "\"\r\n" +
                "Content-Type: image/jpeg\r\n\r\n";  // You can change the content-type depending on the file (e.g., image/png)

        // Send the binary image data
        byte[] headerBytes = body.getBytes();
        byte[] footerBytes = ("\r\n--" + boundary + "--\r\n").getBytes();

        // Prepare the complete body as header + image data + footer
        byte[] completeBody = new byte[headerBytes.length + imageBytes.length + footerBytes.length];
        System.arraycopy(headerBytes, 0, completeBody, 0, headerBytes.length);
        System.arraycopy(imageBytes, 0, completeBody, headerBytes.length, imageBytes.length);
        System.arraycopy(footerBytes, 0, completeBody, headerBytes.length + imageBytes.length, footerBytes.length);

        // Prepare HttpClient and POST request with multipart form data
        HttpClient httpClient = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.imgbb.com/1/upload"))
                .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                .POST(HttpRequest.BodyPublishers.ofByteArray(completeBody))
                .build();


        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        ObjectMapper mapper = new ObjectMapper();

        if(response.statusCode() < 200 || response.statusCode() >= 400){
            Constants.LOGGER.info("Failed to upload image!");
            System.out.println("Raw response:\n" + response.body()); // Debug what you actually got
            return null;
        }

        JsonNode json = mapper.readTree(response.body());
        String link = json.get("data").get("url").asText();

        Constants.LOGGER.info("Uploaded " + image.getName() + ": " + link);

        return link;
    }
}
