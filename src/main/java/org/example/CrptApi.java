package org.example;

import com.google.gson.Gson;

import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

public class CrptApi {
    private final TimeUnit timeUnit;
    private final long timeInterval;
    private final int requestLimit;
    private final ScheduledExecutorService scheduler;
    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final String apiUrl = "https://ismp.crpt.ru/api/v3/lk/documents/create";
    private AtomicInteger requestCounter;
    private volatile long lastRequestTime = System.currentTimeMillis();

    public CrptApi(TimeUnit timeUnit, long timeInterval, int requestLimit) {
        this.timeUnit = timeUnit;
        this.timeInterval = timeInterval;
        this.requestLimit = requestLimit;
        this.scheduler = Executors.newScheduledThreadPool(1);
        requestCounter = new AtomicInteger(0);
    }

    private synchronized boolean makeRequest() {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastRequestTime >= timeConvert(timeUnit, timeInterval)) {
            requestCounter.set(0);
            lastRequestTime = currentTime;
        }
        if (requestCounter.get() < requestLimit) {
            requestCounter.incrementAndGet();
            return true;
        } else {
            return false;
        }
    }

    public synchronized void createDocument(Document document, String signature) {
        if (makeRequest()) {
            Logger.getLogger(CrptApi.class.getName()).log(Level.INFO, "Документ создается");
            scheduler.schedule(() -> {
                sendRequest(document, signature);
            }, 1, timeUnit);

        } else {
            Logger.getLogger(CrptApi.class.getName()).log(Level.INFO, "Превышен лимит запросов");
        }
    }

    private void sendRequest(Document document, String signature) {
        try {
            HttpRequest request = buildRequest(document, signature);
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                String responseBody = response.body();
                processResponseBody(responseBody);
            } else {
                System.out.println("Ошибка: " + response.statusCode());
            }
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
        requestCounter.getAndDecrement();
    }

    private void processResponseBody(String responseBody) {
        // обработка тела ответа
    }

    public void shutdown() {
        scheduler.shutdown();
    }

    private HttpRequest buildRequest(Document document, String signature) {
        String json = convertToJson(document);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(apiUrl))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();
        return request;
    }

    private String convertToJson(Document document) {
        Gson gson = new Gson();
        return gson.toJson(document);
    }

    static class Document {
        private Description description;
        private String docId;
        private String docStatus;
        private String docType;
        private boolean importRequest;
        private String ownerInn;
        private String participantInn;
        private String producerInn;
        private String productionDate;
        private String productionType;
        private List<Product> products;
        private String regDate;
        private String regNumber;
    }

    public class Description {
        public Description(String participantInn) {
            this.participantInn = participantInn;
        }

        public final String participantInn;
    }

    public class Product {
        private String certificateDocument;
        private String certificateDocumentDate;
        private String certificateDocumentNumber;
        private String ownerInn;
        private String producerInn;
        private String productionDate;
        private String tnvedCode;
        private String uitCode;
        private String uituCode;

        public String getCertificateDocument() {
            return certificateDocument;
        }

        public void setCertificateDocument(String certificate_document) {
            this.certificateDocument = certificate_document;
        }

        public String getCertificateDocumentDate() {
            return certificateDocumentDate;
        }

        public void setCertificateDocumentDate(String certificate_document_date) {
            this.certificateDocumentDate = certificate_document_date;
        }

        public String getCertificateDocumentNumber() {
            return certificateDocumentNumber;
        }

        public void setCertificateDocumentNumber(String certificateDocumentNumber) {
            this.certificateDocumentNumber = certificateDocumentNumber;
        }

        public String getOwnerInn() {
            return ownerInn;
        }

        public void setOwnerInn(String ownerInn) {
            this.ownerInn = ownerInn;
        }

        public String getProducerInn() {
            return producerInn;
        }

        public void setProducerInn(String producerInn) {
            this.producerInn = producerInn;
        }

        public String getProductionDate() {
            return productionDate;
        }

        public void setProductionDate(String productionDate) {
            this.productionDate = productionDate;
        }

        public String getTnvedCode() {
            return tnvedCode;
        }

        public void setTnvedCode(String tnvedCode) {
            this.tnvedCode = tnvedCode;
        }

        public String getUitCode() {
            return uitCode;
        }

        public void setUitCode(String uitCode) {
            this.uitCode = uitCode;
        }

        public String getUituCode() {
            return uituCode;
        }

        public void setUituCode(String uituCode) {
            this.uituCode = uituCode;
        }
    }

    private long timeConvert(TimeUnit timeUnit, long interval) {
        switch (timeUnit) {
            case SECONDS:
                return TimeUnit.SECONDS.toMillis(interval);
            case MINUTES:
                return TimeUnit.MILLISECONDS.toMillis(interval);
            case HOURS:
                return TimeUnit.HOURS.toMillis(interval);
            case DAYS:
                return TimeUnit.DAYS.toMillis(interval);
            default:
                throw new IllegalArgumentException("Unsupported TimeUnit: " + timeUnit);
        }
    }
}
