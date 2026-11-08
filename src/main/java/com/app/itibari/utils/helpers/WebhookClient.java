package com.app.itibari.utils.helpers;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

@Slf4j
@Component
public class WebhookClient {

    private static final Duration WEBHOOK_TIMEOUT = Duration.ofSeconds(5);
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(WEBHOOK_TIMEOUT)
            .build();
    private final static String Sales_Webhook_Url = "https://chat.googleapis.com/v1/spaces/AAQAQwdC9OQ/messages?key=AIzaSyDdI0hCZtE6vySjMm-WEfRq3CPzqKqqsHI&token=qgGGkX7Abd7rZWJDkQLaBqJu8OlyNCa6Ql__ppdpGJk";
    private final static String Commercial_Webhook_Url = "https://chat.googleapis.com/v1/spaces/AAQANhnfZxs/messages?key=AIzaSyDdI0hCZtE6vySjMm-WEfRq3CPzqKqqsHI&token=WHRjhDkoCuOUPAuHmxU2UjlL7JXULb0RO3Ab4THrP4Y";
    private final static String Engineering_Webhook_Url = "https://chat.googleapis.com/v1/spaces/AAQApmHwz9s/messages?key=AIzaSyDdI0hCZtE6vySjMm-WEfRq3CPzqKqqsHI&token=kCjg0-2WivEbhhqtJXqyBnqulMXzrJtTeEbJcHca1U4";
    private final static String Warehouse_Webhook_Url = "https://chat.googleapis.com/v1/spaces/AAQAwbWdCnw/messages?key=AIzaSyDdI0hCZtE6vySjMm-WEfRq3CPzqKqqsHI&token=krTmHtl3mj3rJN20Z5EoMko1ZnTNQVYMPL_4vD2PNWI";
    private final static String CX_Webhook_Url = "https://chat.googleapis.com/v1/spaces/AAQAyNtWgeM/messages?key=AIzaSyDdI0hCZtE6vySjMm-WEfRq3CPzqKqqsHI&token=LoGt52KFt71H0EYSoLoCxJrpMUfwExrNeGCroVZ861g";
    private final static String Logistic_Webhook_Url = "https://chat.googleapis.com/v1/spaces/AAQAzXDg5HI/messages?key=AIzaSyDdI0hCZtE6vySjMm-WEfRq3CPzqKqqsHI&token=HskJABj41sKMlxfPgrfHqvLaGletyJRIAVjKhei6Isg";
    private final static String Finance_Webhook_Url = "https://chat.googleapis.com/v1/spaces/AAQA7cDWuhU/messages?key=AIzaSyDdI0hCZtE6vySjMm-WEfRq3CPzqKqqsHI&token=Wogu84QpzYE-TMQPhg3eQf6AZIr5FzxxpTXapwC0SW0";
    private final static String Follow_Up_Webhook_Url = "https://chat.googleapis.com/v1/spaces/AAQAV0G05g8/messages?key=AIzaSyDdI0hCZtE6vySjMm-WEfRq3CPzqKqqsHI&token=typqbMZHgBLI2zciluNdeJTKxX2-W-Ik8br5PdCjUwg";
    private final static String Tra_Errors = "https://chat.googleapis.com/v1/spaces/AAQAYMMdBq8/messages?key=AIzaSyDdI0hCZtE6vySjMm-WEfRq3CPzqKqqsHI&token=rcYQaadd44myA87WPcwtT-zdpXGP8ObsWdThzHO2agA";
    private final static String recon = "https://chat.googleapis.com/v1/spaces/AAQAP0XVmLc/messages?key=AIzaSyDdI0hCZtE6vySjMm-WEfRq3CPzqKqqsHI&token=tVDL4hMYcvSMjErYm_cYFEi-eYYRZsXj7t9UGZrx6RI";
    private final static String Stock_Health = "https://chat.googleapis.com/v1/spaces/AAQA44ZhGig/messages?key=AIzaSyDdI0hCZtE6vySjMm-WEfRq3CPzqKqqsHI&token=Yd31wp90pvYM8SCYQyceWXE9uDc8WlGazZM66s4hHu0";

    public void sendMessage(String webhookUrl, String payload) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(webhookUrl))
                    .timeout(WEBHOOK_TIMEOUT)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(payload))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            System.out.println("Webhook response code: " + response.statusCode());
            System.out.println("Webhook response body: " + response.body());
        } catch (Exception e) {
            log.info("Webhook exception: {}", e.getMessage());
        }
    }

    public void acceptAndSend(String department, String payload){
        if (department.equalsIgnoreCase("Commercials") || department.equalsIgnoreCase("Commercial")){
            sendMessage(Commercial_Webhook_Url, payload);
        }

        if (department.equalsIgnoreCase("Engineering")){
            sendMessage(Engineering_Webhook_Url, payload);
        }

        if (department.equalsIgnoreCase("Warehouse")){
            sendMessage(Warehouse_Webhook_Url, payload);
        }

        if (department.equalsIgnoreCase("CX") || department.equalsIgnoreCase("customer experience")){
            sendMessage(CX_Webhook_Url, payload);
        }

        if (department.equalsIgnoreCase("Logistics") || department.equalsIgnoreCase("Logistic")){
            sendMessage(Logistic_Webhook_Url, payload);
        }

        if (department.equalsIgnoreCase("Finance") || department.equalsIgnoreCase("Accounting")){
            sendMessage(Finance_Webhook_Url, payload);
        }

        if (department.equalsIgnoreCase("sales")){
            sendMessage(Sales_Webhook_Url, payload);
        }

        if (department.equalsIgnoreCase("Tra Errors") || department.equalsIgnoreCase("TRA Errors")){
            sendMessage(Tra_Errors,payload);
        }

        if (department.equalsIgnoreCase("recon")){
            sendMessage(recon,payload);
        }
        if (department.equalsIgnoreCase("Stock Health")){
            sendMessage(Stock_Health, payload);
        }
    }

    public void dmGchartOnFollowUp(String payload) {
        sendMessage(Follow_Up_Webhook_Url, payload);
    }
}
