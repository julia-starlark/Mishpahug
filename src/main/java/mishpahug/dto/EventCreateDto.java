package mishpahug.dto;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonFormat;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EventCreateDto {
	String title;
	String holiday;
	AddressDto address;
	String confession;
	@JsonFormat(pattern="yyyy-MM-dd")
	LocalDate date;
	@JsonFormat(pattern="HH:mm:ss")
	LocalTime time;
	Integer duration;
	List<String> food;
	String description;
}
