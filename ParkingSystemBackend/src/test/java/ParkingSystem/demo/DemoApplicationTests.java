package ParkingSystem.demo;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@TestPropertySource(properties = {
        "app.jwt.secret=dGVzdHNlY3JldGZvcnVuaXR0ZXN0cHVycG9zZW9ubHkxMjM0NTY=",
        "app.jwt.expiration=86400000",
        "app.base-url=http://localhost:8080",
        "spring.mail.host=localhost",
        "spring.mail.username=test@test.com",
        "spring.mail.password=test",
        "spring.datasource.url=jdbc:postgresql://localhost:5432/parkingSystem",
        "spring.jpa.hibernate.ddl-auto=none"
})
class DemoApplicationTests {

    @Test
    void contextLoads() {
    }

}
