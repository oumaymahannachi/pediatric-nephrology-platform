package tn.pedialink.auth.keycloak;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import tn.pedialink.auth.exception.BadRequestException;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class KeycloakService {

    private final RestClient restClient = RestClient.create();

    @Value("${keycloak.server-url}")
    private String serverUrl;

    @Value("${keycloak.realm}")
    private String realm;

    @Value("${keycloak.client-id}")
    private String clientId;

    @Value("${keycloak.admin-username}")
    private String adminUsername;

    @Value("${keycloak.admin-password}")
    private String adminPassword;

    public String getUserToken(String email, String rawPassword) {
        String url = serverUrl + "/realms/" + realm + "/protocol/openid-connect/token";

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "password");
        form.add("client_id", clientId);
        form.add("username", email);
        form.add("password", rawPassword);

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> response = restClient.post()
                    .uri(url)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .body(Map.class);

            if (response == null || !response.containsKey("access_token")) {
                throw new BadRequestException("Failed to obtain token from Keycloak");
            }
            return (String) response.get("access_token");

        } catch (HttpClientErrorException e) {
            log.warn("[KEYCLOAK] Failed to get token for {}: {} {}", email, e.getStatusCode(), e.getResponseBodyAsString());
            throw new BadRequestException("Invalid credentials");
        }
    }

    public void createUser(String email, String rawPassword, String role, String fullName) {
        String adminToken = getAdminToken();

        String firstName = fullName;
        String lastName = "-";
        int spaceIdx = fullName.indexOf(' ');
        if (spaceIdx > 0) {
            firstName = fullName.substring(0, spaceIdx);
            lastName  = fullName.substring(spaceIdx + 1);
        }

        Map<String, Object> credential = Map.of(
                "type", "password",
                "value", rawPassword,
                "temporary", false
        );

        Map<String, Object> userRep = new LinkedHashMap<>();
        userRep.put("username", email);
        userRep.put("email", email);
        userRep.put("firstName", firstName);
        userRep.put("lastName", lastName);
        userRep.put("emailVerified", true);
        userRep.put("enabled", true);
        userRep.put("requiredActions", List.of());
        userRep.put("credentials", List.of(credential));

        String createUrl = serverUrl + "/admin/realms/" + realm + "/users";
        ResponseEntity<Void> createResp = restClient.post()
                .uri(createUrl)
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .body(userRep)
                .retrieve()
                .toBodilessEntity();

        String location = createResp.getHeaders().getFirst("Location");
        if (location == null) {
            throw new RuntimeException("Keycloak user creation: no Location header returned");
        }
        String userId = location.substring(location.lastIndexOf('/') + 1);

        String roleUrl = serverUrl + "/admin/realms/" + realm + "/roles/" + role;
        @SuppressWarnings("unchecked")
        Map<String, Object> roleRep = restClient.get()
                .uri(roleUrl)
                .header("Authorization", "Bearer " + adminToken)
                .retrieve()
                .body(Map.class);

        String assignUrl = serverUrl + "/admin/realms/" + realm + "/users/" + userId + "/role-mappings/realm";
        restClient.post()
                .uri(assignUrl)
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .body(List.of(roleRep))
                .retrieve()
                .toBodilessEntity();

        log.info("[KEYCLOAK] Created user {} with role {}", email, role);
    }

    public void updatePassword(String email, String newPassword) {
        String adminToken = getAdminToken();
        String userId = getUserIdByEmail(email, adminToken);

        String resetUrl = serverUrl + "/admin/realms/" + realm + "/users/" + userId + "/reset-password";
        Map<String, Object> cred = Map.of("type", "password", "value", newPassword, "temporary", false);

        restClient.put()
                .uri(resetUrl)
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .body(cred)
                .retrieve()
                .toBodilessEntity();

        log.info("[KEYCLOAK] Password updated for {}", email);
    }

    public void updateEmailVerified(String email, boolean verified) {
        String adminToken = getAdminToken();
        String userId = getUserIdByEmail(email, adminToken);

        String updateUrl = serverUrl + "/admin/realms/" + realm + "/users/" + userId;
        Map<String, Object> update = Map.of("emailVerified", verified);

        restClient.put()
                .uri(updateUrl)
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .body(update)
                .retrieve()
                .toBodilessEntity();

        log.info("[KEYCLOAK] emailVerified={} for {}", verified, email);
    }

    private String getAdminToken() {
        String url = serverUrl + "/realms/master/protocol/openid-connect/token";

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "password");
        form.add("client_id", "admin-cli");
        form.add("username", adminUsername);
        form.add("password", adminPassword);

        @SuppressWarnings("unchecked")
        Map<String, Object> response = restClient.post()
                .uri(url)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(form)
                .retrieve()
                .body(Map.class);

        if (response == null || !response.containsKey("access_token")) {
            throw new RuntimeException("Could not obtain Keycloak admin token");
        }
        return (String) response.get("access_token");
    }

    private String getUserIdByEmail(String email, String adminToken) {
        String searchUrl = serverUrl + "/admin/realms/" + realm + "/users?email=" + email + "&exact=true";

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> users = restClient.get()
                .uri(searchUrl)
                .header("Authorization", "Bearer " + adminToken)
                .retrieve()
                .body(List.class);

        if (users == null || users.isEmpty()) {
            throw new RuntimeException("User not found in Keycloak: " + email);
        }
        return (String) users.get(0).get("id");
    }
}
