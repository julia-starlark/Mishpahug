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
public class EventsInProgressResponseDto {
	Set<EventResponseDto> content;
	int totalElements;
	int totalPages;
	int size;
	Integer number;
	int numberOfElements;
	boolean first;
	boolean last;
	String sort;
}
