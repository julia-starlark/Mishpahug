package mishpahug.dao;

import java.time.LocalDate;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DynamicQuery {
	LocalDate dateFrom;
	LocalDate dateTo;
	String confession;
	String holiday;
	String food;
	Double lat;
	Double lng;
	Double radius;
	
}
