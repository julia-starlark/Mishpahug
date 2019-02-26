package mishpahug.dto;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationNewDto {
	NotificationTitle title;
	Long eventId;
	String eventTitle;
	LocalDateTime date;
	String userFirstName;
	String userLastName;
}
