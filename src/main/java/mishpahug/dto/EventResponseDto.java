package mishpahug.dto;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;

import org.springframework.data.mongodb.core.mapping.Field;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class EventResponseDto {
	@Field("_id")
	Long eventId;
	String title;
	String holiday;
	String confession;
	@JsonFormat(pattern = "yyyy-MM-dd")
	LocalDate date;
	@JsonInclude(Include.NON_NULL)
	@JsonFormat(pattern = "HH:mm:ss")
	@Setter
	LocalTime time;
	@JsonInclude(Include.NON_NULL)
	@Setter
	Integer duration;
	@JsonInclude(Include.NON_NULL)
	@Setter
	AddressDto address;
	List<String> food;
	String description;
	@JsonInclude(Include.NON_NULL)
	@Setter
	String status;
	@JsonInclude(Include.NON_NULL)
	@Setter
	Set<ParticipantDto> participants;
	@JsonInclude(Include.NON_NULL)
	@Setter
	OwnerDto owner;
}
