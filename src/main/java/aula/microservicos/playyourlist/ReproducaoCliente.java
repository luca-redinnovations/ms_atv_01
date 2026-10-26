package aula.microservicos.playyourlist;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

@FeignClient(name = "reproducoes", url = "${servicos.url}")
public interface ReproducaoCliente {
    @PostMapping("/statistic")
    void registrar(@RequestBody ReproducaoController.Pedido pedido);
}
