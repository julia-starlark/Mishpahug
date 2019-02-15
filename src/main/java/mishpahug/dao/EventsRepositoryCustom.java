package mishpahug.dao;

import org.springframework.data.domain.Page;

import mishpahug.domain.Event;

public interface EventsRepositoryCustom {

	Page<Event> query (DynamicQuery dynamicQuery, int page, int size);
	
		
}
