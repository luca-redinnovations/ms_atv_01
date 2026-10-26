package aula.microservicos.playyourlist;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlaylistMusicaRepository extends JpaRepository<PlaylistMusica, Integer> {
    List<PlaylistMusica> findByPlaylistidOrderByIdAsc(Integer playlistid);
    void deleteByPlaylistid(Integer playlistid);
    void deleteByMusicaid(Integer musicaid);
    void deleteByPlaylistidAndMusicaid(Integer playlistid, Integer musicaid);
}
