package mishpahug.dao;

import java.time.LocalDateTime;
import java.util.List;

import mishpahug.domain.Event;

public interface EventsRepositoryCustom {

	List<Event> query (DynamicQuery dynamicQuery, int page, int size);
	
	boolean checkForEventOverlap(String user, LocalDateTime eventStart, LocalDateTime eventFinish, boolean flag);
	
	void changeEventStatus(long eventId);
	
	//void updateOverLapingEvents(String user, LocalDateTime eventStart, LocalDateTime eventFinish);
}
