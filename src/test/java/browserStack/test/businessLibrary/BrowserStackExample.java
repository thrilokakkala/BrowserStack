package browserStack.test.businessLibrary;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.yaml.snakeyaml.Yaml;

public class BrowserStackExample {

    protected static void openWebPageAndVerifyContent() throws IOException {
        String urlString = "https://elpais.com";
        try {
            URL url = new URL(urlString);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.connect();
            Document document = Jsoup.connect(urlString).get();
            String lang = document.select("html").attr("lang");
            assertTrue("es-ES".equals(lang));
            String title = document.title();
            System.out.println("Page Title: " + title);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void downloadImage(String imageUrl, String imageName) {
        try {
            URL url = new URL(imageUrl);
            InputStream inputStream = url.openStream();
            File outputFile = new File(imageName);
            Path outputPathFile = Paths.get("src/test/resources/" + outputFile);
            Files.createDirectories(outputPathFile.getParent()); // Ensure directories exist
            OutputStream outputStream = new FileOutputStream(outputPathFile.toFile());

            byte[] buffer = new byte[1024];
            int length;
            while ((length = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, length);
            }
            inputStream.close();
            outputStream.close();
            System.out.println("Image saved as: " + imageName);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static HashMap<String, String> scrapeText() {
        HashMap<String, String> scrapetext = new HashMap<>();
        String opinionUrl = "https://elpais.com/opinion/";
        try {
            Document document = Jsoup.connect(opinionUrl).get();
            List<Element> articles = document.select("article");
            // Limit to first 5 articles
            int count = Math.min(5, articles.size());
            for (int i = 0; i < count; i++) {
                Element article = articles.get(i);
                String title = article.select("h2").text();
                String content = article.select("p").text(); // You may need to refine this for full content
                System.out.println("Title: " + title);
                System.out.println("Content: " + content);
                System.out.println();
                scrapetext.put("title" + (i + 1), title);
                // Download and save the cover image if available
                String imgUrl = article.select("img").attr("src"); // Extract image URL
                if (!imgUrl.isEmpty()) {
                    System.out.println("Downloading image from: " + imgUrl);
                    downloadImage(imgUrl, "image_" + (i + 1) + ".jpg");
                } else {
                    System.out.println("No image found for this article.");
                }
                System.out.println("-------------------------------------------------");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return scrapetext;
    }

    public static List<String> translator(HashMap<String, String> text) throws IOException {
        List<String> list = new ArrayList<>();
        text.forEach((key, value) -> {
            try {
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create("https://rapid-translate.p.rapidapi.com/TranslateText"))
                        .header("x-rapidapi-key", "e52a64be55msh15b2439843934dfp1c327bjsn3803fc20912a")
                        .header("x-rapidapi-host", "rapid-translate.p.rapidapi.com")
                        .header("Content-Type", "application/json")
                        .method("POST",
                                HttpRequest.BodyPublishers
                                        .ofString("{\"from\":\"es\",\"text\":\"" + value + "\",\"to\":\"en\"}"))
                        .build();
                HttpResponse<String> resp = HttpClient.newHttpClient().send(request,
                        HttpResponse.BodyHandlers.ofString());
                int responseCode = resp.statusCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    String responseBody = resp.body();
                    System.out.println("Response Body: " + responseBody);
                    String translatedText = parseTranslatedText(responseBody);
                    System.out.println("Translated Text: " + translatedText);
                    list.add(translatedText);
                } else {
                    System.out.println("HTTP error code: " + responseCode);
                }

            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
            }
        });
        System.out.println(list.toString());
        return list;
    }

    private static String parseTranslatedText(String jsonResponse) {
        int startIdx = jsonResponse.indexOf("\"translatedText\":") + 18;
        int endIdx = jsonResponse.indexOf("\"", startIdx);
        return jsonResponse.substring(startIdx, endIdx);
    }

    public static void analyzeData(List<String> translatedText) {
        Map<String, Integer> wordCounts = new HashMap<>();
        for (String title : translatedText) {
            String[] words = title.split(" ");
            for (String word : words) {
                if (!word.equalsIgnoreCase("=")) {
                    word = word.toLowerCase();
                    wordCounts.put(word, wordCounts.getOrDefault(word, 0) + 1);
                }
            }
        }
        for (Map.Entry<String, Integer> entry : wordCounts.entrySet()) {
            if (entry.getValue() >= 2) {
                System.out.println(entry.getKey() + ": repeated " + entry.getValue() + " times");
            }
        }
    }

    @SuppressWarnings("unchecked")
    public static void browserStackRun() throws Exception {
        try {
            InputStream inputStream = BrowserStackExample.class.getClassLoader()
                    .getResourceAsStream("browserstack.yml");
            Yaml yaml = new Yaml();
            Map<String, Object> config = yaml.load(inputStream);
            String username = (String) config.get("userName");
            String accessKey = (String) config.get("accessKey");
            List<Map<String, Object>> capabilitiesList = (List<Map<String, Object>>) config.get("platforms");
            ExecutorService executorService = Executors.newFixedThreadPool(capabilitiesList.size());
            for (Map<String, Object> capabilities : capabilitiesList) {
                executorService.submit(() -> {
                    try {
                        String browser = (String) capabilities.get("browserName");
                        Object browserVersion1 = capabilities.get("browserVersion");
                        String browserVersion = null;
                        if (browserVersion1 != null) {
                            if (browserVersion1 instanceof String) {
                                browserVersion = (String) browserVersion1;
                            } else if (browserVersion1 instanceof Integer) {
                                browserVersion = String.valueOf(browserVersion1);
                            }
                        }
                        String os = (String) capabilities.get("os");
                        Object osVersion1 = capabilities.get("osVersion");
                        String osVersion = null;
                        if (osVersion1 != null) {
                            if (osVersion1 instanceof String) {
                                osVersion = (String) osVersion1;
                            } else if (osVersion1 instanceof Integer) {
                                osVersion = String.valueOf(osVersion1);
                            }
                        }
                        String resolution = (String) capabilities.get("resolution");
                        String device = (String) capabilities.get("deviceName");

                        Object osVer1 = capabilities.get("osVersion");
                        String osVer = null;
                        if (osVer1 != null) {
                            if (osVer1 instanceof String) {
                                osVer = (String) osVer1;
                            } else if (osVer1 instanceof Integer) {
                                osVer = String.valueOf(osVer1);
                            }
                        }
                        DesiredCapabilities desiredCapabilities = new DesiredCapabilities();
                        if (browser != null) {
                            desiredCapabilities.setCapability("browser", browser);
                            desiredCapabilities.setCapability("browser_version", browserVersion);
                            desiredCapabilities.setCapability("os", os);
                            desiredCapabilities.setCapability("os_version", osVersion);
                            desiredCapabilities.setCapability("resolution", resolution);
                        } else if (device != null) {
                            desiredCapabilities.setCapability("device", device);
                            desiredCapabilities.setCapability("os_version", osVer);
                        }
                        desiredCapabilities.setCapability("name", "BrowserStack Parallel Test");
                        String browserStackUrl = "https://" + username + ":" + accessKey
                                + "@hub-cloud.browserstack.com/wd/hub";
                        System.out.println(browserStackUrl);
                        WebDriver driver = new RemoteWebDriver(new URL(browserStackUrl), desiredCapabilities);
                        driver.get("https://elpais.com/opinion/");
                        openWebPageAndVerifyContent();
                        HashMap<String, String> text = scrapeText();
                        List<String> translatedText = translator(text);
                        analyzeData(translatedText);
                        System.out.println(
                                "Test passed on " + (device != null ? device : browser) + ": " + driver.getTitle());
                        driver.quit();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
            }
            executorService.shutdown();
            executorService.awaitTermination(Long.MAX_VALUE, TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
