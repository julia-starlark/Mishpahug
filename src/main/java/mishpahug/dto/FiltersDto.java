package mishpahug.dto;

import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonFormat;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FiltersDto {
	LocationDto location;
	Filters filters;

	@Getter
	@NoArgsConstructor
	@AllArgsConstructor
	public class Filters {
		@JsonFormat(pattern = "yyyy-MM-dd")
		LocalDate dateFrom;
		@JsonFormat(pattern = "yyyy-MM-dd")
		LocalDate dateTo;
		String holidays;
		String confession;
		String food;
	}
}
