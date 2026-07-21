import org.springframework.web.client.RestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public class TestFirebase {
    public static void main(String[] args) {
        try {
            String projectId = "sleepy-frontend-eac65";
            String apiKey = "AIzaSyCko0AeT3hjwvGBlGydpJ-PjA445Txswxw";
            String nickname = "수상한두더지";
            String encodedNickname = URLEncoder.encode(nickname, StandardCharsets.UTF_8.toString());
            
            String url = String.format("https://firestore.googleapis.com/v1/projects/%s/databases/(default)/documents/notifications/%s/userNotifications/999?key=%s", 
                projectId, encodedNickname, apiKey);
                
            System.out.println("URL: " + url);
            
            RestTemplate restTemplate = new RestTemplate();
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("X-HTTP-Method-Override", "PATCH");
            
            String json = "{\"fields\": {\"id\": {\"integerValue\": 999}, \"type\": {\"stringValue\": \"NEW_COMMENT\"}, \"message\": {\"stringValue\": \"test\"}, \"relatedUrl\": {\"stringValue\": \"\"}, \"isRead\": {\"booleanValue\": false}, \"createdAt\": {\"integerValue\": 12345}}}";
            
            HttpEntity<String> entity = new HttpEntity<>(json, headers);
            
            System.out.println("Sending request...");
            String response = restTemplate.exchange(url, HttpMethod.POST, entity, String.class).getBody();
            System.out.println("Response: " + response);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
