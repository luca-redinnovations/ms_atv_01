package aula.microservicos.playyourlist;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class ApiController {
    private final MusicaCliente musicas;
    private final PlaylistCliente playlists;
    private final ReproducaoCliente reproducoes;
    public ApiController(MusicaCliente musicas, PlaylistCliente playlists, ReproducaoCliente reproducoes) {
        this.musicas = musicas;
        this.playlists = playlists;
        this.reproducoes = reproducoes;
    }
    @PostMapping("/adicionar/{playlistId}/musicas/{musicaId}")
    public String adicionar(@PathVariable Integer playlistId, @PathVariable Integer musicaId) {
        var musica = musicas.obter(musicaId);
        var playlist = playlists.obter(playlistId);
        playlists.adicionar(playlistId, musicaId);
        return "Música " + musica.titulo + " adicionada com sucesso à playlist " + playlist.nome;
    }
    @PutMapping("/executar/{playlistId}")
    public void executar(@PathVariable Integer playlistId) {
        playlists.obter(playlistId);
        reproducoes.registrar(new ReproducaoController.Pedido(playlistId));
    }
}
