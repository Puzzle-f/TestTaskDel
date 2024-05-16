package org.example;


import com.google.gson.Gson;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;

public class Main {
    public static void main(String[] args) throws IOException, InterruptedException {
        CrptApi api = new CrptApi(TimeUnit.SECONDS, 5, 2);
        String signature = "signature";
        String json;

        byte[] bytes = Files.readAllBytes(Paths.get("src/main/resources/document.json"));
        json = new String(bytes, StandardCharsets.UTF_8);

        Gson gson = new Gson();
        CrptApi.Document document = gson.fromJson(json, CrptApi.Document.class);
        synchronized (api) {
            api.createDocument(document, signature);
            api.createDocument(document, signature);
            api.createDocument(document, signature);
            api.createDocument(document, signature);
            Thread.sleep(6000);
            api.createDocument(document, signature);
        }
        api.shutdown();
    }
}