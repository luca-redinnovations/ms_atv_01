package aula.microservicos.playyourlist;

import java.util.List;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.web.bind.annotation.*;

@RestController
public class ReproducaoController {
    public record Pedido(@NotNull Integer playlistid) {}
    private final PlaylistService service;
    private final ReproducaoRepository repository;
    public ReproducaoController(PlaylistService service, ReproducaoRepository repository) {
        this.service = service;
        this.repository = repository;
    }
    @PostMapping({"/reproducao", "/statistic"})
    public Reproducao registrar(@Valid @RequestBody Pedido pedido) { return service.registrar(pedido.playlistid()); }
    @GetMapping("/reproducao/{playlistid}")
    public List<Reproducao> listar(@PathVariable Integer playlistid) { return repository.findByPlaylistidOrderByIdAsc(playlistid); }
    @GetMapping("/reproducao/total/{playlistid}")
    public long total(@PathVariable Integer playlistid) { return repository.countByPlaylistid(playlistid); }
}
