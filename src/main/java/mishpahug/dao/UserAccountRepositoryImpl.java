package mishpahug.dao;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

import mishpahug.domain.User;

public class UserAccountRepositoryImpl implements UserAccountRepositoryCustome {
	private final MongoTemplate mongoTemplate;

	@Autowired
	public UserAccountRepositoryImpl(MongoTemplate mongoTemplate) {
		this.mongoTemplate = mongoTemplate;
	}

	@Override
	public String getUserFullName(String login) {
		Query query = new Query();
		query.addCriteria(Criteria.where("_id").is(login));
		String name = mongoTemplate.findDistinct(query, "firstName", User.class, String.class).get(0);
		String lastname = mongoTemplate.findDistinct(query, "lastName", User.class, String.class).get(0);
		return name + " " + lastname;
	}

}
