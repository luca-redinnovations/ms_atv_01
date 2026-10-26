package aula.microservicos.playyourlist;

import java.util.List;
import java.time.LocalDateTime;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class PlaylistService {
    private final MusicaRepository musicas;
    private final PlaylistRepository playlists;
    private final PlaylistMusicaRepository vinculos;
    private final ReproducaoRepository reproducoes;

    public PlaylistService(MusicaRepository musicas, PlaylistRepository playlists,
            PlaylistMusicaRepository vinculos, ReproducaoRepository reproducoes) {
        this.musicas = musicas;
        this.playlists = playlists;
        this.vinculos = vinculos;
        this.reproducoes = reproducoes;
    }

    public Musica musica(Integer id) {
        return musicas.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Música inexistente"));
    }

    public Playlist playlist(Integer id) {
        return playlists.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Playlist inexistente"));
    }

    @Transactional
    public void excluirMusica(Integer id) {
        musica(id);
        vinculos.deleteByMusicaid(id);
        musicas.deleteById(id);
    }

    @Transactional
    public void excluirPlaylist(Integer id) {
        playlist(id);
        vinculos.deleteByPlaylistid(id);
        playlists.deleteById(id);
    }

    @Transactional
    public void adicionar(Integer playlistid, Integer musicaid) {
        playlist(playlistid);
        musica(musicaid);
        var vinculo = new PlaylistMusica();
        vinculo.playlistid = playlistid;
        vinculo.musicaid = musicaid;
        vinculos.save(vinculo);
    }

    @Transactional
    public void remover(Integer playlistid, Integer musicaid) {
        playlist(playlistid);
        musica(musicaid);
        vinculos.deleteByPlaylistidAndMusicaid(playlistid, musicaid);
    }

    public List<Integer> musicasDaPlaylist(Integer playlistid) {
        playlist(playlistid);
        return vinculos.findByPlaylistidOrderByIdAsc(playlistid).stream().map(v -> v.musicaid).toList();
    }

    public Reproducao registrar(Integer playlistid) {
        playlist(playlistid);
        var reproducao = new Reproducao();
        reproducao.playlistid = playlistid;
        reproducao.datahora = LocalDateTime.now();
        return reproducoes.save(reproducao);
    }
}
