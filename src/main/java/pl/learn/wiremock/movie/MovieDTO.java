package pl.learn.wiremock.movie;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Builder;

import java.time.LocalDate;

@Builder
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record MovieDTO(Long movieId,
                       String cast,
                       String name,
                       LocalDate releaseDate,
                       Integer year) {
}
