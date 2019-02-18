package mishpahug.dao;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import mishpahug.domain.Event;
import mishpahug.dto.EventDateTimeDto;
import mishpahug.dto.EventForCalendarDto;

public interface EventsRepository extends MongoRepository<Event, Long>, EventsRepositoryCustom {

	public List<EventDateTimeDto> findEventDateTimeByOwner(String owner);

	//{$or: [{'date':{$gte:?0}},{'date':{$lte:?1}}]}
	@Query(value = "{$and: [{$or:[{'owner':?2},{'subscribers':?2}]},{$or: [{'date':{$gte:?0,$lte:?1}}]}]}")
	public List<EventForCalendarDto> findEventByMonth(LocalDate dateFrom, LocalDate dateTo, String login);

	public List<Event> findEventByOwner(String owner);
	
	@Query("{$and:[{$or:[{'dateTimeStart':{$gte:?1,$lte:?2}},{$and:[{'dateTimeStart':{$lte:?1}},{'dateTimeFinish':{$gte:?1}}]}]},{'subscribers':?0}]}")
	public List<Event> findOverlapingEvents(String user, LocalDateTime dateFrom, LocalDateTime dateTo);
	
	@Query("{'subscribers':?0}")
	public List<Event> findEventBySubscribers(String user);
	
	//FIXME how to get boolean
	@Query("{$and:[{'eventId':{$in:?0}},{'date':?1}]}")
	public List<Event> findOverlapByDate(Set<Long> eventId, LocalDate date);
	
	@Query("{'date':{$gte:?0}}")
	public List<Event> findEventsByDateFrom(LocalDate dateFrom);
	
	@Query("{'date':{$lte:?0}}")
	public List<Event> findEventsByDateTo(LocalDate dateTo);
	
}
