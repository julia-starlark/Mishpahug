package mishpahug.dao;

import java.util.List;
import java.util.Set;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import mishpahug.domain.User;

public interface UserAccountRepository extends MongoRepository<User, String> {

	@Query(value = "{$and: [{'_id':{$in : ?0}},{'invitations':{$elemMatch : {$eq: ?1}}}]}", fields="{'login':'_id'}")
	List<User> findParticipants( Set<String> subscribers, long eventId);
	
	User findUserByUserId(long userId);
	
}
