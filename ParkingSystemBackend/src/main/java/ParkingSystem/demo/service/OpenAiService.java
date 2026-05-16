package ParkingSystem.demo.service;

import ParkingSystem.demo.dto.recommendation.RecommendationResponse;
import ParkingSystem.demo.dto.weather.WeatherInfo;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;

@Service
public class OpenAiService {

    private final RestClient restClient;
    private final String apiKey;
    private final String model;

    public OpenAiService(
            RestClient.Builder restClientBuilder,
            @Value("${openai.api-key}") String apiKey,
            @Value("${openai.model}") String model) {
        this.restClient = restClientBuilder.build();
        this.apiKey = apiKey;
        this.model = model;
    }

    public String generateReason(RecommendationResponse draft, WeatherInfo weather) {
        String userContent = String.format(
                "Zone %d has %d available spots with %.0f%% predicted availability. " +
                "Weather: %s (%s), %.1f°C. Original suggestion: %s.",
                draft.zoneId(), draft.availableSpots(),
                draft.predictedProbability() * 100,
                weather.condition(), weather.description(), weather.tempCelsius(),
                draft.reason());

        ChatRequest request = new ChatRequest(model, List.of(
                new ChatMessage("system",
                        "You are a parking assistant. Respond with exactly one concise sentence " +
                        "(max 100 characters) recommending a parking zone."),
                new ChatMessage("user", userContent)
        ), 60);

        ChatResponse response = restClient.post()
                .uri("https://api.openai.com/v1/chat/completions")
                .header("Authorization", "Bearer " + apiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .body(ChatResponse.class);

        return response.choices().get(0).message().content().trim();
    }

    private record ChatRequest(
            String model,
            List<ChatMessage> messages,
            @JsonProperty("max_tokens") int maxTokens) {}

    private record ChatMessage(String role, String content) {}
    private record ChatResponse(List<Choice> choices) {}
    private record Choice(Message message) {}
    private record Message(String content) {}
}
