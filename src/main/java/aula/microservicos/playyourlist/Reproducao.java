package aula.microservicos.playyourlist;

import java.time.LocalDateTime;
import jakarta.persistence.*;

@Entity
@Table(name = "reproducoes")
public class Reproducao {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Integer id;
    @Column(nullable = false)
    public Integer playlistid;
    @Column(nullable = false)
    public LocalDateTime datahora;
}
