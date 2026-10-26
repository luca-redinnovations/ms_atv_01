# Atividade 1 — Play Your List

Implementação de [MS-AV-01-Endpoints](https://github.com/esensato/ms-2026-02/blob/main/MS-AV-01-Endpoints.md), seguindo os exemplos de Spring Boot, Spring Data JPA, H2, Bean Validation e OpenFeign do [repositório da disciplina](https://github.com/esensato/ms-2026-02).

Os quatro grupos — músicas, playlists, reproduções e API — estão no mesmo projeto, conforme o enunciado. O texto de referência está em [ENUNCIADO.md](ENUNCIADO.md).

## Executar

Requisito: Java 17 ou superior. O Maven Wrapper está incluído.

```sh
./mvnw spring-boot:run
```

No Windows:

```powershell
.\mvnw.cmd spring-boot:run
```

A aplicação atende em `http://localhost:8080`. Para mudar a porta, defina a variável de ambiente `PORT`. A URL dos clientes Feign acompanha `server.port`, permitindo as chamadas HTTP entre os grupos dentro da mesma aplicação.

O banco H2 em memória é criado por `src/main/resources/data.sql` a cada inicialização. As quatro tabelas e os registros são os fornecidos no enunciado: **5 músicas, 5 playlists, 10 associações e 12 reproduções**. Os `CREATE TABLE` e `INSERT` foram preservados. Os `DROP TABLE` foram colocados no início, removendo primeiro `playlist_musicas`, para respeitar as chaves estrangeiras.

## Endpoints

| Método | Caminho | Comportamento |
|---|---|---|
| POST | `/musicas` | Cadastrar música |
| GET | `/musicas` | Listar músicas |
| GET | `/musicas/{id}` | Buscar música |
| PUT | `/musicas/{id}` | Atualizar música |
| DELETE | `/musicas/{id}` | Excluir música e remover seus vínculos |
| POST | `/playlists` | Criar playlist |
| GET | `/playlists` | Listar playlists |
| GET | `/playlists/{playlistid}` | Buscar playlist |
| PUT | `/playlists/{playlistid}` | Atualizar nome e descrição |
| DELETE | `/playlists/{playlistid}` | Excluir playlist e seus vínculos com músicas |
| POST | `/playlists/{playlistid}/musicas/{musicaId}` | Associar música |
| DELETE | `/playlists/{playlistid}/musicas/{musicaId}` | Remover associação |
| GET | `/playlists/{playlistid}/musicas` | Listar os IDs das músicas |
| POST | `/reproducao` | Registrar reprodução |
| GET | `/reproducao/{playlistid}` | Listar reproduções |
| GET | `/reproducao/total/{playlistid}` | Contar reproduções |
| POST | `/api/adicionar/{playlistId}/musicas/{musicaId}` | Validar e associar recursos via OpenFeign |
| PUT | `/api/executar/{playlistId}` | Registrar execução via OpenFeign em `POST /statistic` |

O próprio enunciado usa `/reproducao` na lista de endpoints e exige `POST /statistic` na descrição de `/api/executar`. Por isso, **`POST /statistic` é um alias do mesmo método de `POST /reproducao`**: ambos registram na tabela `reproducoes`. O cliente `ReproducaoCliente` chama literalmente `/statistic`.

Ao excluir uma playlist, são removidas as linhas correspondentes em `playlist_musicas`. As músicas permanecem na tabela `musicas`, pois podem estar associadas a outras playlists.

## Corpos das requisições

Cadastro e alteração de música, com o exemplo do enunciado:

```json
{
  "titulo": "Hotel California",
  "artista": "Eagles",
  "album": "Hotel California",
  "duracao": 391,
  "genero": "Rock"
}
```

Cadastro e alteração de playlist, exemplo de teste:

```json
{
  "nome": "Verificação funcional",
  "descricao": "Playlist temporária da verificação"
}
```

Registro de reprodução em `/reproducao` ou `/statistic`:

```json
{"playlistid": 1}
```

O servidor gera `id` e `datahora`. Os endpoints de associação e os dois endpoints `/api` recebem os IDs pelo caminho e não precisam de corpo.

## OpenFeign e validações

`MusicaCliente` consulta a música por HTTP. `PlaylistCliente` consulta a playlist por HTTP e efetua a associação. A resposta de `/api/adicionar` segue a frase exigida; por exemplo, para a música 2 e a playlist 1:

```text
Música Billie Jean adicionada com sucesso à playlist Clássicos do Rock
```

`/api/executar` consulta a playlist e envia o registro por `ReproducaoCliente` para `/statistic`. Recursos inexistentes retornam 404 e impedem a associação ou execução solicitada.

As validações são aplicadas tanto no cadastro quanto na alteração:

| Recurso | Campo | Regra |
|---|---|---|
| Música | `titulo`, `artista` | Obrigatórios, rejeitando vazio e somente espaços; até 150 caracteres, conforme o SQL |
| Música | `album` | Opcional; até 150 caracteres |
| Música | `duracao` | Obrigatória e maior que zero |
| Música | `genero` | Opcional; até 50 caracteres |
| Playlist | `nome` | Obrigatório, rejeitando vazio e somente espaços; até 100 caracteres, conforme o SQL |
| Playlist | `descricao` | Opcional; até 255 caracteres |

Requisições que violam essas regras retornam 400. As operações bem-sucedidas retornam 200. IDs novos são gerados pelo banco.

## Comprovação funcional

```sh
./mvnw clean verify
```

**46 testes aprovados, 0 falhas, 0 erros e 0 ignorados.** O teste abre um servidor Tomcat e um banco H2 reais. Cada caso começa com os dados originais do `data.sql`.

| O que foi verificado | Como foi comprovado |
|---|---|
| Dados iniciais | Quantidades, nomes, associações e totais de reprodução do enunciado |
| Todos os 18 endpoints | Requisições HTTP e conferência dos dados retornados/persistidos |
| CRUDs e vínculos | Criação, leitura, alteração, exclusão e preservação de músicas compartilhadas |
| OpenFeign em `/api/adicionar` | Registro das chamadas HTTP a `/musicas`, `/playlists` e ao endpoint de associação; conferência da frase exata |
| OpenFeign em `/api/executar` | Registro da chamada HTTP a `POST /statistic`; total aumenta exatamente uma vez |
| IDs inexistentes | Resposta 404 sem inclusão de associações ou reproduções inválidas |
| Validações de músicas | 26 casos inválidos, cobrindo cadastro e alteração |
| Validações de playlists | 10 casos inválidos, cobrindo cadastro e alteração |
| Limites e opcionais | Aceitação dos limites permitidos e dos campos opcionais ausentes ou vazios |

Resultados do Maven e lista dos casos: [evidencias/testes.txt](evidencias/testes.txt).

Também foi executado o **JAR compilado**, com requisições HTTP e verificações dos resultados. As requisições, corpos e respostas reais estão em [evidencias/funcional-http.txt](evidencias/funcional-http.txt).

Os testes estão em `src/test/java/aula/microservicos/playyourlist/FuncionalTests.java`. Os clientes Feign e os repositórios usados nos testes são os da aplicação; as chamadas aos serviços não foram substituídas por mocks.

## Entrega

Repositório: https://github.com/luca-redinnovations/ms_atv_01
