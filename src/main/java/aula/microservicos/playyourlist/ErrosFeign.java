package aula.microservicos.playyourlist;

import feign.FeignException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestControllerAdvice
public class ErrosFeign {
    @ExceptionHandler(FeignException.class)
    public ResponseEntity<String> tratar(FeignException erro) {
        int status = erro.status() >= 400 && erro.status() <= 599 ? erro.status() : 502;
        return ResponseEntity.status(status).body("Falha na chamada ao serviço: HTTP " + status);
    }
}
