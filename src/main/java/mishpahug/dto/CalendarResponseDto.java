package mishpahug.dto;

import java.util.Set;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CalendarResponseDto {
	Set<EventForCalendarDto> myEvents;
	Set<EventForCalendarDto> subscribedEvents;
}
