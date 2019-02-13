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
public class NotificationDto implements Comparable<NotificationDto>{
	Long notificationId;
	String title;
	String message;
	@JsonFormat(pattern="yyyy-MM-dd")
	LocalDate date;
	String type;
	boolean isRead;
	Long eventId;
	
	@Override
	public int compareTo(NotificationDto other) {
		int res = 0;
		if(this.isRead == false && other.isRead == true) {
			res = 1;
		}
		if(this.isRead == true && other.isRead == false) {
			res = -1;
		} else {
		res = other.getDate().compareTo(this.getDate());
		}
		return res;
	}
}
