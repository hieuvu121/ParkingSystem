package ParkingSystem.demo.service;

import ParkingSystem.demo.dto.weather.WeatherInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;

@Slf4j
@Service
public class WeatherService {

    private final RestClient restClient;
    private final String apiKey;
    private final double defaultLat;
    private final double defaultLng;

    public WeatherService(
            RestClient.Builder restClientBuilder,
            @Value("${weather.api-key}") String apiKey,
            @Value("${weather.default-lat}") double defaultLat,
            @Value("${weather.default-lng}") double defaultLng) {
        this.restClient = restClientBuilder.build();
        this.apiKey = apiKey;
        this.defaultLat = defaultLat;
        this.defaultLng = defaultLng;
    }

    public WeatherInfo getWeather(Double lat, Double lng) {
        double reqLat = (lat != null) ? lat : defaultLat;
        double reqLng = (lng != null) ? lng : defaultLng;

        OWMResponse response = restClient.get()
                .uri("https://api.openweathermap.org/data/2.5/weather?lat={lat}&lon={lng}&appid={key}&units=metric",
                        reqLat, reqLng, apiKey)
                .retrieve()
                .body(OWMResponse.class);

        OWMWeatherItem item = response.weather().get(0);
        return new WeatherInfo(item.main(), item.description(), response.main().temp());
    }

    private record OWMResponse(List<OWMWeatherItem> weather, OWMMain main) {}
    private record OWMWeatherItem(String main, String description) {}
    private record OWMMain(double temp) {}
}
