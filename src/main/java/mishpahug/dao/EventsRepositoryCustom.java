package mishpahug.dao;

import java.time.LocalDateTime;

import org.springframework.data.domain.Page;

import mishpahug.domain.Event;

public interface EventsRepositoryCustom {

	Page<Event> query (DynamicQuery dynamicQuery, int page, int size);
	
	boolean checkForEventOverlap(String user, LocalDateTime eventStart, LocalDateTime eventFinish, boolean flag);
	
	void changeEventStatus(long eventId);
	
	//void updateOverLapingEvents(String user, LocalDateTime eventStart, LocalDateTime eventFinish);
}
