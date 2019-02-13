package mishpahug.domain;

import java.time.LocalDate;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@NoArgsConstructor
@EqualsAndHashCode(of = {"notificationId"})
@ToString
public class Notification {
	long notificationId;
	String title;
	String message;
	LocalDate date;
	String type;
	@Setter
	boolean isRead;
	Long eventId;
	
	public Notification(String title, String message, Long eventId) {
		this.notificationId = System.currentTimeMillis();
		this.title = title;
		this.message = message;
		this.eventId = eventId;
		this.date = LocalDate.now();
		this.type = "system";
	}
	
	
}
