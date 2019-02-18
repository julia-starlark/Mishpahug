package mishpahug.dao;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import mishpahug.domain.Event;
import mishpahug.domain.EventArchive;
import mishpahug.dto.EventResponseDto;

public interface ArchiveRepository extends MongoRepository<EventArchive, Long> {

	//@Query(fields="{'title':'title','holiday':'holiday','confession':'confession','date':'date','food':'food','description':'description','status':'status'}")
	List<Event> findByOwner(String owner);
	
	//FIXME fields
	@Query(value="{$and: [{'participants': ?0},{'voted':{$nin:[?0]}},{'status': 'done'}]}", 
			fields="{'title':'title','holiday':'holiday','address':'addres','confession':'confession','date':'date','description':'description','status':'status', 'owner':'owner'}")
	List<Event> findEventsByParticipants(String userLogin);
}
