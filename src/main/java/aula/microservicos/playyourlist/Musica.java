package aula.microservicos.playyourlist;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;

@Entity
@Table(name = "musicas")
public class Musica {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Integer id;
    @NotBlank @Size(max = 150) @Column(length = 150, nullable = false)
    public String titulo;
    @NotBlank @Size(max = 150) @Column(length = 150, nullable = false)
    public String artista;
    @Size(max = 150) @Column(length = 150)
    public String album;
    @NotNull @Positive @Column(nullable = false)
    public Integer duracao;
    @Size(max = 50) @Column(length = 50)
    public String genero;
}
