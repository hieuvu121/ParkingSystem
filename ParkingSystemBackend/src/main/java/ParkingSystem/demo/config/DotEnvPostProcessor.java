package ParkingSystem.demo.config;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public class DotEnvPostProcessor implements EnvironmentPostProcessor {

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        Path dotEnv = resolveEnvFile();
        if (dotEnv == null) return;

        Map<String, Object> props = new HashMap<>();
        try {
            for (String line : Files.readAllLines(dotEnv)) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) continue;
                int idx = line.indexOf('=');
                if (idx > 0) {
                    props.put(line.substring(0, idx).trim(), line.substring(idx + 1).trim());
                }
            }
        } catch (IOException ignored) {
            return;
        }

        // lowest priority — real OS env vars still win
        environment.getPropertySources().addLast(new MapPropertySource("dotenv", props));
    }

    private Path resolveEnvFile() {
        Path workDir = Path.of(System.getProperty("user.dir"));
        // walk up up to 3 levels looking for .env
        Path dir = workDir;
        for (int i = 0; i <= 3; i++) {
            Path candidate = dir.resolve(".env");
            if (Files.exists(candidate)) return candidate;
            Path parent = dir.getParent();
            if (parent == null) break;
            dir = parent;
        }
        return null;
    }
}
