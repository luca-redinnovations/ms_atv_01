package aula.microservicos.playyourlist;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients
public class PlayYourListApplication {
    public static void main(String[] args) {
        SpringApplication.run(PlayYourListApplication.class, args);
    }
}
