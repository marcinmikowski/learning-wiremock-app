package pl.learn.wiremock.movie;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.common.ConsoleNotifier;
import com.github.tomakehurst.wiremock.common.ContentTypes;
import com.github.tomakehurst.wiremock.extension.TemplateModelDataProviderExtension;
import com.github.tomakehurst.wiremock.http.HttpHeader;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.http.codec.json.Jackson2JsonDecoder;
import org.springframework.http.codec.json.Jackson2JsonEncoder;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;
import wiremock.org.apache.hc.client5.http.classic.methods.HttpHead;
import wiremock.org.apache.hc.core5.http.HttpHeaders;
import wiremock.org.apache.hc.core5.http.HttpStatus;

import javax.net.ssl.SSLException;
import java.io.File;
import java.time.LocalDate;
import java.time.Month;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.ok;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;

@WireMockTest
class MovieWebClientTest {
    private static final String GET_ALL_MOVIES_V1 = "/movieservice/v1/allMovies";
    private static final String GET_MOVIE_BY_ID_V1 = "/movieservice/v1/movie/{id}";
    private static final String GET_MOVIE_BY_MOVIE_NAME_V1 = "/movieservice/v1/movieName";
    private static final String GET_MOVIE_BY_MOVIE_YEAR_V1 = "/movieservice/v1/movieYear";
    private static final String CREATE_MOVIE_V1 = "/movieservice/v1/movie";
    private static final String UPDATE_MOVIE_V1 = "/movieservice/v1/movie/{id}";
    private static final String DELETE_MOVIE_BY_ID_V1 = "/movieservice/v1/movie/{id}";

    private static final SslContext sslContext;

    static {
        try {
            sslContext = SslContextBuilder
                    .forClient()
                    .trustManager(InsecureTrustManagerFactory.INSTANCE)
                    .build();
        } catch (SSLException e) {
            throw new RuntimeException(e);
        }
    }

    private static final HttpClient httpClient = HttpClient.create().secure(t -> t.sslContext(sslContext));

    @RegisterExtension
    private static final WireMockExtension wme = WireMockExtension.newInstance()
            .options(wireMockConfig().bindAddress("localhost")
                    .notifier(new ConsoleNotifier(true))
                    .globalTemplating(true)
                    .dynamicHttpsPort()
                    .keystorePath(
                            new File(Objects.requireNonNull(MovieWebClientTest.class.getClassLoader()
                                    .getResource("ssl/keystore.jks")).getPath()).getAbsolutePath())
                    .keystorePassword("password")
                    .keystoreType("JKS")
            )
            .configureStaticDsl(true)
            .build();

    private final ObjectMapper mapper = (new ObjectMapper())
            .setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE)
            .registerModules(new JavaTimeModule(), new Jdk8Module());

    private final ExchangeStrategies exchangeStrategies = ExchangeStrategies.builder()
            .codecs(configurer -> {
                configurer.defaultCodecs().jackson2JsonDecoder(new Jackson2JsonDecoder(mapper));
                configurer.defaultCodecs().jackson2JsonEncoder(new Jackson2JsonEncoder(mapper));
            }).build();

    private final WebClient webClient = WebClient.builder()
            .clientConnector(new ReactorClientHttpConnector(httpClient))
            .exchangeStrategies(exchangeStrategies)
            .baseUrl(wme.baseUrl()).build();

    private final MovieWebClient movieWebClient = new MovieWebClient(webClient);

    @Test
    void retrieveAllMovies() {
        // given
        WireMock.stubFor(get(urlEqualTo(GET_ALL_MOVIES_V1))
                .willReturn(aResponse()
                        .withHeader(HttpHeaders.CONTENT_TYPE, ContentTypes.APPLICATION_JSON)
                        .withBodyFile("get-all-movies-200.json")));

        // when
        List<MovieDTO> movieList = movieWebClient.retrieveAllMovies();

        // then
        assertThat(movieList).size().isGreaterThan(0);

        System.out.println(movieList);
    }

    @Test
    void retrieveMovieById() {
        // given
        Long id = 2L;

        // when
        Optional<MovieDTO> movie = movieWebClient.retrieveMovieById(id);

        // then
        assertThat(movie).isNotNull();

        System.out.println(movie);
    }

    @Test
    void retrieveMovieByMovieName() {
        // given
        String movieName = "Avengers: Infinity Wars";

        // when
        List<MovieDTO> movie = movieWebClient.retrieveMovieByMovieName(movieName);

        // then
        assertThat(movie).isNotNull();

        System.out.println(movie);
    }

    @Test
    void retrieveMovieByMovieYear() {
        // given
        Integer year = 2008;

        // when
        List<MovieDTO> movie = movieWebClient.retrieveMovieByMovieYear(year);

        // then
        assertThat(movie).isNotNull();

        System.out.println(movie);
    }

    @Test
    void createMovie() {
        // given
        MovieDTO movie = MovieDTO.builder()
                .name("The Sin")
                .cast("John Doe")
                .year(2020)
                .releaseDate(LocalDate.of(2016, Month.AUGUST, 12))
                .build();

        // when
        MovieDTO savedMovie = movieWebClient.createMovie(movie);

        // then
        assertThat(savedMovie).isNotNull();

        System.out.println(savedMovie);
    }

    @Test
    void updateMovie() {
        // given
        MovieDTO movie = MovieDTO.builder()
                .name("Smart signs")
                .cast("Johanes Braun")
                .year(2011)
                .releaseDate(LocalDate.of(2012, Month.FEBRUARY, 20))
                .build();

        System.out.println(movie);

        Long movieId = 11L;

        // when
        MovieDTO savedMovie = movieWebClient.updateMovie(movieId, movie);

        // then
        assertThat(savedMovie).isNotNull();

        System.out.println(savedMovie);
    }

    @Test
    void deleteMovie() {
        // given
        Long movieId = 13L;

        // when
        movieWebClient.deleteMovie(movieId);

        // then
        Optional<MovieDTO> movie = movieWebClient.retrieveMovieById(movieId);

        assertThat(movie).isEmpty();
    }

}