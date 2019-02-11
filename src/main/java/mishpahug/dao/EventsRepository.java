package mishpahug.dao;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import mishpahug.domain.Event;
import mishpahug.dto.EventDateTimeDto;
import mishpahug.dto.EventForCalendarDto;

public interface EventsRepository extends MongoRepository<Event, Long> {

	public List<EventDateTimeDto> findEventDateTimeByOwner(String owner);

	//{$or: [{'date':{$gte:?0}},{'date':{$lte:?1}}]}
	@Query(value = "{$and: [{$or:[{'owner':?2},{'participants':?2}]},{$or: [{'date':{$gte:?0,$lte:?1}}]}]}")
	public List<EventForCalendarDto> findEventByMonth(LocalDate dateFrom, LocalDate dateTo, String login);

	public List<Event> findEventByOwner(String owner);
	
	@Query("{$and:[{'date': ?1},{'participants':?0}]}")
	public List<Event> findDateOverlapForUser(String user, LocalDate date);
	
	@Query("{'participants':?0}")
	public List<Event> findEventByParticipant(String participant);
	
	//FIXME how to get boolean
	@Query("{$and:[{'eventId':{$in:?0}},{'date':?1}]}")
	public List<Event> findOverlapByDate(Set<Long> eventId, LocalDate date);
	
}
