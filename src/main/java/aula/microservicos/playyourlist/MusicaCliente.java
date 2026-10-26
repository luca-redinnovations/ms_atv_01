package aula.microservicos.playyourlist;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

@FeignClient(name = "musicas", url = "${servicos.url}")
public interface MusicaCliente {
    @GetMapping("/musicas/{id}")
    Musica obter(@PathVariable("id") Integer id);
}
