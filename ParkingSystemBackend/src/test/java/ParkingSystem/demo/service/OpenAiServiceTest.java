package ParkingSystem.demo.service;

import ParkingSystem.demo.dto.recommendation.RecommendationResponse;
import ParkingSystem.demo.dto.weather.WeatherInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.http.HttpMethod.POST;

class OpenAiServiceTest {

    private OpenAiService openAiService;
    private MockRestServiceServer server;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        openAiService = new OpenAiService(builder, "test-key", "gpt-4o-mini");
    }

    @Test
    void generateReason_withWeather_returnsAiReason() {
        server.expect(requestTo(containsString("api.openai.com/v1/chat/completions")))
                .andExpect(method(POST))
                .andRespond(withSuccess("""
                        {"choices":[{"message":{"content":"Head to Zone 3 before the rain gets heavier."}}]}
                        """, MediaType.APPLICATION_JSON));

        RecommendationResponse draft = new RecommendationResponse(3L, "Nearest available zone", 5, 0.82);
        WeatherInfo weather = new WeatherInfo("Rain", "light rain", 28.5);

        String result = openAiService.generateReason(draft, weather);

        assertThat(result).isEqualTo("Head to Zone 3 before the rain gets heavier.");
        server.verify();
    }

    @Test
    void generateReason_serverError_throwsException() {
        server.expect(requestTo(containsString("api.openai.com")))
                .andRespond(withServerError());

        RecommendationResponse draft = new RecommendationResponse(1L, "Least congested zone", 3, 0.6);
        WeatherInfo weather = new WeatherInfo("Clear", "clear sky", 31.0);

        assertThatThrownBy(() -> openAiService.generateReason(draft, weather))
                .isInstanceOf(Exception.class);
    }
}
