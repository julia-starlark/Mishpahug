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
public class NotificationDto {
	Long notificationId;
	String title;
	String message;
	@JsonFormat(pattern="yyyy-MM-dd")
	LocalDate date;
	String type;
	boolean isRead;
	Long eventId;
}
