package aula.microservicos.playyourlist;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

@FeignClient(name = "playlists", url = "${servicos.url}")
public interface PlaylistCliente {
    @GetMapping("/playlists/{id}")
    Playlist obter(@PathVariable("id") Integer id);
    @PostMapping("/playlists/{playlistid}/musicas/{musicaId}")
    void adicionar(@PathVariable("playlistid") Integer playlistid, @PathVariable("musicaId") Integer musicaId);
}
