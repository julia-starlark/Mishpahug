package mishpahug.dao;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import mishpahug.domain.EventArchive;
import mishpahug.dto.EventResponseDto;

public interface ArchiveRepository extends MongoRepository<EventArchive, Long> {

	@Query(fields="{'title':'title','holiday':'holiday','confession':'confession','date':'date','food':'food','description':'description','status':'status'}")
	List<EventResponseDto> findByOwner(String owner);
	
	//FIXME fields
	@Query(fields="{'title':'title','holiday':'holiday','confession':'confession','date':'date','description':'description','status':'status', 'owner':'owner'}")
	List<EventResponseDto> findEventsByParticipantsContains(String userLogin);
}
