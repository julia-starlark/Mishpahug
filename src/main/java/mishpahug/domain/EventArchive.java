package mishpahug.domain;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Getter
@NoArgsConstructor
@EqualsAndHashCode(of = {"eventId"})
@Document(collection = "archieve")
@ToString
@AllArgsConstructor
@Builder
public class EventArchive {
	@Id
	long eventId;
	String title;
	String holiday;
	String confession;
	LocalDate date;
	LocalTime time;
	int duration;
	Address address;
	List<String> food;
	String description;
	String status;
	Set<String> participants;
	String owner;

}
