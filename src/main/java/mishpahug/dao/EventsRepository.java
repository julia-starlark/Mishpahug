package mishpahug.dao;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import mishpahug.domain.Event;

public interface EventsRepository extends MongoRepository<Event, Long>, EventsRepositoryCustom {

	//public List<EventDateTimeDto> findEventDateTimeByOwner(String owner);

	@Query(value = "{$and: [{$or:[{'owner':?2},{'subscribers':?2}]},{$or: [{'dateTimeStart':{$gte:?0,$lte:?1}}]}]}")
	public List<Event> findEventByMonth(LocalDateTime dateFrom, LocalDateTime dateTo, String login);

	public List<Event> findEventByOwner(String owner);
	
	@Query("{$and:[{$or:[{'dateTimeStart':{$gte:?1,$lte:?2}},{$and:[{'dateTimeStart':{$lte:?1}},{'dateTimeFinish':{$gte:?1}}]}]},{'subscribers':?0}]}")
	public List<Event> findOverlapingEvents(String user, LocalDateTime dateFrom, LocalDateTime dateTo);
	
	@Query("{'subscribers':?0}")
	public List<Event> findEventBySubscribers(String user);
	}
		
