package aula.microservicos.playyourlist;

import java.net.*;
import java.net.http.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.stream.Stream;
import jakarta.servlet.Filter;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.jdbc.Sql;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@Sql("/data.sql")
class FuncionalTests {
    static final int PORT = portaLivre();
    static final Queue<String> CHAMADAS = new ConcurrentLinkedQueue<>();
    static final JsonMapper JSON = JsonMapper.builder().build();
    final HttpClient http = HttpClient.newHttpClient();
    @Autowired JdbcTemplate jdbc;

    static int portaLivre() {
        try (var socket = new ServerSocket(0)) { return socket.getLocalPort(); }
        catch (Exception e) { throw new IllegalStateException(e); }
    }
    @DynamicPropertySource
    static void propriedades(DynamicPropertyRegistry registry) {
        registry.add("server.port", () -> PORT);
        registry.add("servicos.url", () -> "http://127.0.0.1:" + PORT);
        registry.add("spring.datasource.url", () -> "jdbc:h2:mem:testes;DB_CLOSE_DELAY=-1");
    }
    @TestConfiguration(proxyBeanMethods = false)
    static class RastroHttp {
        @Bean FilterRegistrationBean<Filter> registrarChamadas() {
            var registration = new FilterRegistrationBean<Filter>();
            registration.setFilter((request, response, chain) -> {
                var req = (jakarta.servlet.http.HttpServletRequest) request;
                CHAMADAS.add(req.getMethod() + " " + req.getRequestURI());
                chain.doFilter(request, response);
            });
            return registration;
        }
    }
    @BeforeEach void limparRastro() { CHAMADAS.clear(); }

    HttpResponse<String> chamar(String metodo, String caminho, Object corpo) throws Exception {
        var builder = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + PORT + caminho))
                .timeout(Duration.ofSeconds(15));
        if (corpo != null) builder.header("Content-Type", "application/json");
        return http.send(builder.method(metodo, corpo == null ? HttpRequest.BodyPublishers.noBody()
                : HttpRequest.BodyPublishers.ofString(JSON.writeValueAsString(corpo))).build(), HttpResponse.BodyHandlers.ofString());
    }
    JsonNode ok(String metodo, String caminho, Object corpo) throws Exception {
        var resposta = chamar(metodo, caminho, corpo);
        assertEquals(200, resposta.statusCode(), metodo + " " + caminho + " " + resposta.body());
        return resposta.body().isBlank() ? null : JSON.readTree(resposta.body());
    }
    static Map<String, Object> musica() {
        var m = new HashMap<String, Object>();
        m.put("titulo", "Hotel California"); m.put("artista", "Eagles");
        m.put("album", "Hotel California"); m.put("duracao", 391); m.put("genero", "Rock");
        return m;
    }
    static Map<String, Object> playlist() {
        var p = new HashMap<String, Object>();
        p.put("nome", "Teste funcional"); p.put("descricao", "Playlist criada pelo teste");
        return p;
    }

    @Test void dadosIniciaisIguaisAoEnunciado() throws Exception {
        var musicas = ok("GET", "/musicas", null);
        assertEquals(5, musicas.size());
        assertEquals("Imagine", musicas.get(0).get("titulo").asText());
        assertEquals("John Lennon", musicas.get(0).get("artista").asText());
        assertEquals("Billie Jean", musicas.get(1).get("titulo").asText());
        assertEquals("Bohemian Rhapsody", musicas.get(2).get("titulo").asText());
        assertEquals("Garota de Ipanema", musicas.get(3).get("titulo").asText());
        assertEquals("Tempo Perdido", musicas.get(4).get("titulo").asText());
        assertEquals(5, ok("GET", "/playlists", null).size());
        assertEquals("Clássicos do Rock", ok("GET", "/playlists/1", null).get("nome").asText());
        assertEquals("[1,3,5]", ok("GET", "/playlists/1/musicas", null).toString());
        assertEquals(10, jdbc.queryForObject("SELECT COUNT(*) FROM playlist_musicas", Integer.class));
        int[] totais = {5,3,2,1,1};
        for (int i = 0; i < totais.length; i++) assertEquals(totais[i], ok("GET", "/reproducao/total/" + (i+1), null).asInt());
    }

    @Test void crudCompletoDeMusicas() throws Exception {
        var nova = ok("POST", "/musicas", musica());
        int id = nova.get("id").asInt();
        assertEquals(6, id);
        assertEquals("Hotel California", ok("GET", "/musicas/" + id, null).get("titulo").asText());
        var alterada = musica(); alterada.put("titulo", "Hotel California ao vivo");
        ok("PUT", "/musicas/" + id, alterada);
        assertEquals("Hotel California ao vivo", ok("GET", "/musicas/" + id, null).get("titulo").asText());
        assertEquals(6, ok("GET", "/musicas", null).size());
        ok("DELETE", "/musicas/" + id, null);
        assertEquals(404, chamar("GET", "/musicas/" + id, null).statusCode());
        assertEquals(5, ok("GET", "/musicas", null).size());
    }

    @Test void excluirMusicaVinculadaSemViolarChavesEstrangeiras() throws Exception {
        ok("DELETE", "/musicas/1", null);
        assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM playlist_musicas WHERE musicaid=1", Integer.class));
        assertEquals(7, jdbc.queryForObject("SELECT COUNT(*) FROM playlist_musicas", Integer.class));
        assertEquals(4, ok("GET", "/musicas", null).size());
        assertEquals(5, ok("GET", "/playlists", null).size());
    }

    @Test void crudDePlaylistsEAssociacoes() throws Exception {
        int id = ok("POST", "/playlists", playlist()).get("id").asInt();
        assertEquals(6, id);
        var alterada = playlist(); alterada.put("nome", "Nome alterado"); alterada.put("descricao", "Descrição alterada");
        ok("PUT", "/playlists/" + id, alterada);
        var resultado = ok("GET", "/playlists/" + id, null);
        assertEquals("Nome alterado", resultado.get("nome").asText());
        assertEquals("Descrição alterada", resultado.get("descricao").asText());
        ok("POST", "/playlists/" + id + "/musicas/1", null);
        assertEquals("[1]", ok("GET", "/playlists/" + id + "/musicas", null).toString());
        ok("DELETE", "/playlists/" + id + "/musicas/1", null);
        assertEquals(0, ok("GET", "/playlists/" + id + "/musicas", null).size());
        ok("POST", "/playlists/" + id + "/musicas/2", null);
        ok("DELETE", "/playlists/" + id, null);
        assertEquals(404, chamar("GET", "/playlists/" + id, null).statusCode());
        assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM playlist_musicas WHERE playlistid=?", Integer.class, id));
        assertEquals(5, ok("GET", "/playlists", null).size());
        assertEquals(5, ok("GET", "/musicas", null).size());
        assertEquals("[2]", ok("GET", "/playlists/3/musicas", null).toString());
    }

    @Test void excluirPlaylistOriginalPreservaMusicasCompartilhadas() throws Exception {
        ok("DELETE", "/playlists/1", null);
        assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM playlist_musicas WHERE playlistid=1", Integer.class));
        assertEquals(5, ok("GET", "/musicas", null).size());
        assertEquals("[1,4]", ok("GET", "/playlists/4/musicas", null).toString());
    }

    @Test void registrarListarEContarReproducoes() throws Exception {
        var criada = ok("POST", "/reproducao", Map.of("playlistid", 1));
        assertEquals(13, criada.get("id").asInt());
        assertEquals(1, criada.get("playlistid").asInt());
        assertNotNull(LocalDateTime.parse(criada.get("datahora").asText()));
        assertEquals(6, ok("GET", "/reproducao/1", null).size());
        assertEquals(6, ok("GET", "/reproducao/total/1", null).asInt());
        assertEquals(404, chamar("POST", "/reproducao", Map.of("playlistid", 999)).statusCode());
        assertEquals(12+1, jdbc.queryForObject("SELECT COUNT(*) FROM reproducoes", Integer.class));
    }

    @Test void apiAdicionaComMensagemExataEChamadasHttpFeign() throws Exception {
        var resposta = chamar("POST", "/api/adicionar/1/musicas/2", null);
        assertEquals(200, resposta.statusCode());
        assertEquals("Música Billie Jean adicionada com sucesso à playlist Clássicos do Rock", resposta.body());
        assertTrue(CHAMADAS.containsAll(List.of("GET /musicas/2", "GET /playlists/1", "POST /playlists/1/musicas/2")), CHAMADAS.toString());
        assertEquals(11, jdbc.queryForObject("SELECT COUNT(*) FROM playlist_musicas", Integer.class));
        assertEquals("[1,3,5,2]", ok("GET", "/playlists/1/musicas", null).toString());
        assertEquals(404, chamar("POST", "/api/adicionar/1/musicas/999", null).statusCode());
        assertEquals(404, chamar("POST", "/api/adicionar/999/musicas/1", null).statusCode());
        assertEquals(11, jdbc.queryForObject("SELECT COUNT(*) FROM playlist_musicas", Integer.class));
    }

    @Test void apiExecutaViaPostStatisticComOpenFeign() throws Exception {
        ok("PUT", "/api/executar/1", null);
        assertTrue(CHAMADAS.containsAll(List.of("GET /playlists/1", "POST /statistic")), CHAMADAS.toString());
        assertEquals(6, ok("GET", "/reproducao/total/1", null).asInt());
        assertEquals(6, ok("GET", "/reproducao/1", null).size());
        assertEquals(404, chamar("PUT", "/api/executar/999", null).statusCode());
        assertEquals(13, jdbc.queryForObject("SELECT COUNT(*) FROM reproducoes", Integer.class));
    }

    @Test void recursosInexistentesNaoAlteramDados() throws Exception {
        for (String metodo : List.of("GET", "PUT", "DELETE")) {
            assertEquals(404, chamar(metodo, "/musicas/999", metodo.equals("PUT") ? musica() : null).statusCode());
            assertEquals(404, chamar(metodo, "/playlists/999", metodo.equals("PUT") ? playlist() : null).statusCode());
        }
        assertEquals(404, chamar("POST", "/playlists/999/musicas/1", null).statusCode());
        assertEquals(404, chamar("POST", "/playlists/1/musicas/999", null).statusCode());
        assertEquals(404, chamar("GET", "/playlists/999/musicas", null).statusCode());
        assertEquals(5, jdbc.queryForObject("SELECT COUNT(*) FROM musicas", Integer.class));
        assertEquals(5, jdbc.queryForObject("SELECT COUNT(*) FROM playlists", Integer.class));
        assertEquals(10, jdbc.queryForObject("SELECT COUNT(*) FROM playlist_musicas", Integer.class));
    }

    static Stream<Arguments> musicasInvalidas() {
        var casos = new ArrayList<Arguments>();
        for (String metodo : List.of("POST", "PUT")) {
            for (String campo : List.of("titulo", "artista")) {
                for (String valor : Arrays.asList(null, "", "   ", "x".repeat(151))) casos.add(Arguments.of(metodo, campo, valor));
            }
            for (Integer valor : Arrays.asList(null, 0, -1)) casos.add(Arguments.of(metodo, "duracao", valor));
            casos.add(Arguments.of(metodo, "album", "x".repeat(151)));
            casos.add(Arguments.of(metodo, "genero", "x".repeat(51)));
        }
        return casos.stream();
    }
    @ParameterizedTest(name = "{0} música inválida: {1}={2}")
    @MethodSource("musicasInvalidas")
    void validarMusicaNoCadastroENaAlteracao(String metodo, String campo, Object valor) throws Exception {
        var corpo = musica(); corpo.put(campo, valor);
        assertEquals(400, chamar(metodo, metodo.equals("POST") ? "/musicas" : "/musicas/1", corpo).statusCode());
        assertEquals(5, jdbc.queryForObject("SELECT COUNT(*) FROM musicas", Integer.class));
        assertEquals("Imagine", ok("GET", "/musicas/1", null).get("titulo").asText());
    }
    static Stream<Arguments> playlistsInvalidas() {
        var casos = new ArrayList<Arguments>();
        for (String metodo : List.of("POST", "PUT")) {
            for (String valor : Arrays.asList(null, "", "   ", "x".repeat(101))) casos.add(Arguments.of(metodo, "nome", valor));
            casos.add(Arguments.of(metodo, "descricao", "x".repeat(256)));
        }
        return casos.stream();
    }
    @ParameterizedTest(name = "{0} playlist inválida: {1}={2}")
    @MethodSource("playlistsInvalidas")
    void validarPlaylistNoCadastroENaAlteracao(String metodo, String campo, Object valor) throws Exception {
        var corpo = playlist(); corpo.put(campo, valor);
        assertEquals(400, chamar(metodo, metodo.equals("POST") ? "/playlists" : "/playlists/1", corpo).statusCode());
        assertEquals(5, jdbc.queryForObject("SELECT COUNT(*) FROM playlists", Integer.class));
        assertEquals("Clássicos do Rock", ok("GET", "/playlists/1", null).get("nome").asText());
    }

    @Test void limitesPermitidosECamposOpcionais() throws Exception {
        var m = musica(); m.put("album", "x".repeat(150)); m.put("genero", "x".repeat(50)); m.put("duracao", 1);
        int id = ok("POST", "/musicas", m).get("id").asInt();
        m.remove("album"); m.remove("genero");
        ok("PUT", "/musicas/" + id, m);
        assertTrue(ok("GET", "/musicas/" + id, null).get("album").isNull());
        m.put("album", ""); m.put("genero", ""); ok("PUT", "/musicas/" + id, m);
        var p = playlist(); p.put("nome", "x".repeat(100)); p.put("descricao", "x".repeat(255));
        int pid = ok("POST", "/playlists", p).get("id").asInt();
        p.remove("descricao"); ok("PUT", "/playlists/" + pid, p);
        assertTrue(ok("GET", "/playlists/" + pid, null).get("descricao").isNull());
    }
}
