package aula.microservicos.playyourlist;

import java.util.List;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/playlists")
public class PlaylistController {
    private final PlaylistRepository repository;
    private final PlaylistService service;
    public PlaylistController(PlaylistRepository repository, PlaylistService service) {
        this.repository = repository;
        this.service = service;
    }
    @PostMapping
    public Playlist cadastrar(@Valid @RequestBody Playlist playlist) {
        playlist.id = null;
        return repository.save(playlist);
    }
    @GetMapping
    public List<Playlist> listar() { return repository.findAll(); }
    @GetMapping("/{playlistid}")
    public Playlist obter(@PathVariable Integer playlistid) { return service.playlist(playlistid); }
    @PutMapping("/{playlistid}")
    public Playlist atualizar(@PathVariable Integer playlistid, @Valid @RequestBody Playlist playlist) {
        service.playlist(playlistid);
        playlist.id = playlistid;
        return repository.save(playlist);
    }
    @DeleteMapping("/{playlistid}")
    public void excluir(@PathVariable Integer playlistid) { service.excluirPlaylist(playlistid); }
    @PostMapping("/{playlistid}/musicas/{musicaId}")
    public void adicionar(@PathVariable Integer playlistid, @PathVariable Integer musicaId) { service.adicionar(playlistid, musicaId); }
    @DeleteMapping("/{playlistid}/musicas/{musicaId}")
    public void remover(@PathVariable Integer playlistid, @PathVariable Integer musicaId) { service.remover(playlistid, musicaId); }
    @GetMapping("/{playlistid}/musicas")
    public List<Integer> listarMusicas(@PathVariable Integer playlistid) { return service.musicasDaPlaylist(playlistid); }
}
