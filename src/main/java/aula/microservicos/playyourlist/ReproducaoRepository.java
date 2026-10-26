package aula.microservicos.playyourlist;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReproducaoRepository extends JpaRepository<Reproducao, Integer> {
    List<Reproducao> findByPlaylistidOrderByIdAsc(Integer playlistid);
    long countByPlaylistid(Integer playlistid);
}
