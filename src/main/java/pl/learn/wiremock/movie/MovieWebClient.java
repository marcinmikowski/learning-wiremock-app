package pl.learn.wiremock.movie;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatusCode;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RequiredArgsConstructor
public class MovieWebClient {

    private static final String GET_ALL_MOVIES_V1 = "/movieservice/v1/allMovies";
    private static final String GET_MOVIE_BY_ID_V1 = "/movieservice/v1/movie/{id}";
    private static final String GET_MOVIE_BY_MOVIE_NAME_V1 = "/movieservice/v1/movieName";
    private static final String GET_MOVIE_BY_MOVIE_YEAR_V1 = "/movieservice/v1/movieYear";
    private static final String CREATE_MOVIE_V1 = "/movieservice/v1/movie";
    private static final String UPDATE_MOVIE_V1 = "/movieservice/v1/movie/{id}";
    private static final String DELETE_MOVIE_BY_ID_V1 = "/movieservice/v1/movie/{id}";

    private final WebClient webClient;

    public List<MovieDTO> retrieveAllMovies() {
        return webClient.get().uri(GET_ALL_MOVIES_V1)
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, x -> Mono.empty())
                .bodyToFlux(MovieDTO.class)
                .collectList()
                .block();
    }

    public Optional<MovieDTO> retrieveMovieById(Long id) {
        return webClient.get().uri(uriBuilder -> uriBuilder.path(GET_MOVIE_BY_ID_V1)
                        .build(Map.of("id", id)))
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, x -> Mono.empty())
                .bodyToMono(MovieDTO.class)
                .onErrorComplete()
                .blockOptional();
    }

    public List<MovieDTO> retrieveMovieByMovieName(String movieName) {
        return webClient.get().uri(uriBuilder -> uriBuilder.path(GET_MOVIE_BY_MOVIE_NAME_V1)
                        .queryParam("movie_name", movieName)
                        .build())
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, x -> Mono.empty())
                .bodyToFlux(MovieDTO.class)
                .onErrorComplete()
                .collectList()
                .block();
    }

    public List<MovieDTO> retrieveMovieByMovieYear(Integer year) {
        return webClient.get().uri(uriBuilder -> uriBuilder.path(GET_MOVIE_BY_MOVIE_YEAR_V1)
                        .queryParam("year", year)
                        .build())
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, x -> Mono.empty())
                .bodyToFlux(MovieDTO.class)
                .onErrorComplete()
                .collectList()
                .block();
    }

    public MovieDTO createMovie(MovieDTO movie) {
        return webClient.post().uri(CREATE_MOVIE_V1)
                        .bodyValue(movie)
                        .retrieve()
                        .bodyToMono(MovieDTO.class)
                        .block();
    }

    public MovieDTO updateMovie(Long id, MovieDTO movie) {
        return webClient.put().uri(uriBuilder -> uriBuilder.path(UPDATE_MOVIE_V1)
                        .build(Map.of("id", id)))
                .bodyValue(movie)
                .retrieve()
                .bodyToMono(MovieDTO.class)
                .block();
    }

    public void deleteMovie(Long id) {
        webClient.delete()
                .uri(DELETE_MOVIE_BY_ID_V1, id)
                .retrieve()
                .toBodilessEntity()
                .block();
    }

}
