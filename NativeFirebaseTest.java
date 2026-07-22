import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class NativeFirebaseTest {
    public static void main(String[] args) throws Exception {
        String projectId = "sleepy-frontend-eac65";
        String apiKey = "AIzaSyCko0AeT3hjwvGBlGydpJ-PjA445Txswxw";
        String encodedNickname = "testuser";
        long notificationId = 123;
        String urlStr = String.format("https://firestore.googleapis.com/v1/projects/%s/databases/(default)/documents/notifications/%s/userNotifications/%d?updateMask.fieldPaths=isRead&key=%s", 
            projectId, encodedNickname, notificationId, apiKey);
        
        String jsonBody = "{\"fields\": {\"isRead\": {\"booleanValue\": true}}}";
        
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(urlStr))
                .header("Content-Type", "application/json")
                .header("X-HTTP-Method-Override", "PATCH")
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();
                
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        System.out.println("Status: " + response.statusCode());
        System.out.println("Response: " + response.body());
    }
}
