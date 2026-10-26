package aula.microservicos.playyourlist;

import java.util.List;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/musicas")
public class MusicaController {
    private final MusicaRepository repository;
    private final PlaylistService service;
    public MusicaController(MusicaRepository repository, PlaylistService service) {
        this.repository = repository;
        this.service = service;
    }
    @PostMapping
    public Musica cadastrar(@Valid @RequestBody Musica musica) {
        musica.id = null;
        return repository.save(musica);
    }
    @GetMapping
    public List<Musica> listar() { return repository.findAll(); }
    @GetMapping("/{id}")
    public Musica obter(@PathVariable Integer id) { return service.musica(id); }
    @PutMapping("/{id}")
    public Musica atualizar(@PathVariable Integer id, @Valid @RequestBody Musica musica) {
        service.musica(id);
        musica.id = id;
        return repository.save(musica);
    }
    @DeleteMapping("/{id}")
    public void excluir(@PathVariable Integer id) { service.excluirMusica(id); }
}
