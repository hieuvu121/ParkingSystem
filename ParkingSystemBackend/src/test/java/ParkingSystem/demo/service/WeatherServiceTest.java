package ParkingSystem.demo.service;

import ParkingSystem.demo.dto.weather.WeatherInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.hamcrest.Matchers.containsString;

class WeatherServiceTest {

    private WeatherService weatherService;
    private MockRestServiceServer server;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        weatherService = new WeatherService(builder, "test-key", 10.7769, 106.7009);
    }

    @Test
    void getWeather_withCoords_parsesAndReturnsWeatherInfo() {
        server.expect(requestTo(containsString("api.openweathermap.org/data/2.5/weather")))
                .andRespond(withSuccess("""
                        {"weather":[{"main":"Rain","description":"light rain"}],"main":{"temp":28.5}}
                        """, MediaType.APPLICATION_JSON));

        WeatherInfo result = weatherService.getWeather(10.77, 106.70);

        assertThat(result.condition()).isEqualTo("Rain");
        assertThat(result.description()).isEqualTo("light rain");
        assertThat(result.tempCelsius()).isEqualTo(28.5);
        server.verify();
    }

    @Test
    void getWeather_nullCoords_usesDefaultCoords() {
        server.expect(requestTo(containsString("lat=10.7769")))
                .andRespond(withSuccess("""
                        {"weather":[{"main":"Clear","description":"clear sky"}],"main":{"temp":31.0}}
                        """, MediaType.APPLICATION_JSON));

        WeatherInfo result = weatherService.getWeather(null, null);

        assertThat(result.condition()).isEqualTo("Clear");
        server.verify();
    }

    @Test
    void getWeather_serverError_throwsException() {
        server.expect(requestTo(containsString("api.openweathermap.org")))
                .andRespond(withServerError());

        assertThatThrownBy(() -> weatherService.getWeather(10.0, 106.0))
                .isInstanceOf(Exception.class);
    }
}
