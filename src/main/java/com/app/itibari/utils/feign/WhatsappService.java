//package com.app.itibari.utils.feign;
//
//import com.fasterxml.jackson.databind.JsonNode;
//import com.fasterxml.jackson.databind.ObjectMapper;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.http.*;
//import org.springframework.stereotype.Service;
//import org.springframework.web.client.RestTemplate;
//
//@Service
//public class WhatsappService {
//
//    @Value("${whatsapp.phoneNumberId}")
//    private String phoneNumberId;
//
//    @Value("${whatsapp.token}")
//    private String accessToken;
//
//    private final String groupId = "1234567890";
//
//    private final RestTemplate restTemplate = new RestTemplate();
//    private final ObjectMapper mapper = new ObjectMapper();
//
//    private final String API_URL = "https://graph.facebook.com/v20.0"; // Update to latest version
//
//    public void sendTextToGroup(String message) {
//        String url = API_URL + "/" + phoneNumberId + "/messages";
//
//        JsonNode payload = mapper.createObjectNode()
//                .put("messaging_product", "whatsapp")
//                .put("to", groupId + "@g.us")  //
//                .put("type", "text")
//                .set("text", mapper.createObjectNode().put("body", message));
//
//        sendMessage(payload, url);
//    }
//
//    public void sendImageToGroup(String groupId, String imageUrl, String caption) {
//        String url = API_URL + "/" + phoneNumberId + "/messages";
//
//        JsonNode payload = mapper.createObjectNode()
//                .put("messaging_product", "whatsapp")
//                .put("to", groupId + "@g.us")
//                .put("type", "image")
//                .set("image", mapper.createObjectNode()
//                        .put("link", imageUrl)  // Public URL to image
//                        .put("caption", caption));
//
//        sendMessage(payload, url);
//    }
//
//
//    private void sendMessage(JsonNode payload, String url) {
//        HttpHeaders headers = new HttpHeaders();
//        headers.setContentType(MediaType.APPLICATION_JSON);
//        headers.setBearerAuth(accessToken);
//
//        HttpEntity<String> entity = new HttpEntity<>(payload.toString(), headers);
//
//        ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);
//        // Log response or handle errors
//        System.out.println("WhatsApp Response: " + response.getBody());
//    }
//}
