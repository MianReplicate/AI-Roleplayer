package discord.mian.custom;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import javax.imageio.ImageIO;
import java.io.*;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.List;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class Util {
    public static final List<String> imageExtensions = List.of(".jpg", ".jpeg", ".png", ".gif", ".webp");

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
        if(!data.exists()) {
            data.mkdir();
        }

        // ensures there's a default folder
        File defaultFolder = new File("data\\defaults");
        if(!new File("data\\defaults").exists()){
            try (InputStream in = Util.class.getClassLoader().getResourceAsStream("defaults.zip")) {
                assert in != null;
                ZipInputStream zip = new ZipInputStream(in);
                ZipEntry entry;

                while ((entry = zip.getNextEntry()) != null) {
                    // entry.getName() is a path
                    Path outPath = defaultFolder.toPath().resolve(entry.getName());

                    // creates directory if entry is a directory
                    if (entry.isDirectory()) {
                        Files.createDirectories(outPath);
                    } else {
                        // creates directory for file and then copies the file
                        Files.createDirectories(outPath.getParent());
                        Files.copy(zip, outPath, StandardCopyOption.REPLACE_EXISTING);
                    }
                    zip.closeEntry();
                }
            } catch (IOException ignored) {
                Constants.LOGGER.info("Failed to create default folder!");
            }
        }

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

    public static boolean isValidImage(File image) throws IOException {
        if(!image.exists())
            return false;

        String name = image.getName();
        String extension = name.substring(name.lastIndexOf("."));

        return imageExtensions.contains(extension) && ImageIO.read(image) != null;
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

    public static void copyDirectory(Path source, Path target) throws IOException {
        // Walk the file tree starting from the source path
        Files.walk(source).forEach(sourcePath -> {
            try {
                Path targetPath = target.resolve(source.relativize(sourcePath));
                if (Files.isDirectory(sourcePath)) {
                    Files.createDirectories(targetPath);
                } else {
                    Files.copy(sourcePath, targetPath, StandardCopyOption.REPLACE_EXISTING);
                }
            } catch (IOException e) {
                throw new RuntimeException("Failed to copy: " + sourcePath, e);
            }
        });
    }
}
