package aula.microservicos.playyourlist;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;

@Entity
@Table(name = "playlists")
public class Playlist {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Integer id;
    @NotBlank @Size(max = 100) @Column(length = 100, nullable = false)
    public String nome;
    @Size(max = 255) @Column(length = 255)
    public String descricao;
}
