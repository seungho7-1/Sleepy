import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.HttpStatusCodeException;

public class TestFirebase {
    public static void main(String[] args) {
        try {
            String projectId = "sleepy-frontend-eac65";
            String apiKey = "AIzaSyCko0AeT3hjwvGBlGydpJ-PjA445Txswxw";
            String encodedNickname = "testuser";
            long notificationId = 123;
            String urlStr = String.format("https://firestore.googleapis.com/v1/projects/%s/databases/(default)/documents/notifications/%s/userNotifications/%d?updateMask.fieldPaths=isRead&key=%s", 
                projectId, encodedNickname, notificationId, apiKey);
            URI uri = URI.create(urlStr);

            RestTemplate restTemplate = new RestTemplate();
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("X-HTTP-Method-Override", "PATCH");

            Map<String, Object> body = new HashMap<>();
            Map<String, Object> fields = new HashMap<>();
            fields.put("isRead", Map.of("booleanValue", true));
            body.put("fields", fields);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

            restTemplate.exchange(uri, HttpMethod.POST, entity, String.class);
            System.out.println("Success!");
        } catch (HttpStatusCodeException e) {
            System.out.println("HTTP Error: " + e.getStatusCode());
            System.out.println("Response Body: " + e.getResponseBodyAsString());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
