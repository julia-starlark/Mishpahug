package mishpahug.dao;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

import mishpahug.domain.Notification;
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

	@Override
	public void addNotificationToUser(String login, Notification notification) {
		Query query = new Query();
		query.addCriteria(Criteria.where("_id").is(login));
		Update update = new Update().addToSet("notifications", notification);
		mongoTemplate.findAndModify(query, update, User.class);
	}

	

}
