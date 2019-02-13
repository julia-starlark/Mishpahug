package mishpahug.dto;

import java.util.List;

import org.springframework.data.domain.Sort;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EventsInProgressResponseDto {
	List<EventResponseDto> content;
	int totalElements;
	int totalPages;
	int size;
	Integer number;
	int numberOfElements;
	boolean first;
	boolean last;
	Sort sort;
}
