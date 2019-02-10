package mishpahug.domain;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@NoArgsConstructor
@EqualsAndHashCode(of = {"eventId"})
@Document(collection = "events")
@ToString
public class Event {
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
	@Setter
	String status;
	Set<String> participants;
	Set<String> voted;
	String owner;

	public Event(String title, String holiday, String confession, LocalDate date, LocalTime time, int duration,
			Address address, List<String> food, String description, String owner) {
		this.eventId = System.currentTimeMillis();
		this.title = title;
		this.holiday = holiday;
		this.confession = confession;
		this.date = date;
		this.time = time;
		this.duration = duration;
		this.address = address;
		this.food = food;
		this.description = description;
		this.status = "in progress";
		this.participants = new HashSet<>();
		this.voted = new HashSet<>();
		this.owner = owner;
	}

	public boolean addParticipant(String login) {
		 participants.add(login);
		 return true;
	}

	public boolean deleteParticipant(String login) {
		return participants.remove(login);
	}
}
