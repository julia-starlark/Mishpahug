package mishpahug.dao;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.geo.Point;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.data.repository.support.PageableExecutionUtils;

import mishpahug.domain.Event;

public class EventsRepositoryImpl implements EventsRepositoryCustom {

	private final MongoTemplate mongoTemplate;

	@Autowired
	public EventsRepositoryImpl(MongoTemplate mongoTemplate) {
		this.mongoTemplate = mongoTemplate;
	}

	@Override
	public Page<Event> query(DynamicQuery dynamicQuery, int page, int size) {
		final Query query = new Query();
		Pageable pageableRequest = PageRequest.of(page, size);
		final List<Criteria> criteria = new ArrayList<>();
		//FIXME
		if (dynamicQuery.getDateFrom() != null) {
			LocalDateTime from = LocalDateTime.of(dynamicQuery.getDateFrom(), LocalTime.of(0,0,0));
			System.out.println(from);
			criteria.add(Criteria.where("dateTimeStart").gte(from));
		}
		if (dynamicQuery.getDateTo() != null) {
			LocalDateTime to = LocalDateTime.of(dynamicQuery.getDateTo(), LocalTime.of(23,59,59));
			System.out.println(to);
			criteria.add(Criteria.where("dateTimeStart").lte(to));
		}
		if (!dynamicQuery.getConfession().equals("")) {
			criteria.add(Criteria.where("confession").is(dynamicQuery.getConfession()));
		}
		if (!dynamicQuery.getHoliday().equals("")) {
			criteria.add(Criteria.where("holiday").is(dynamicQuery.getHoliday()));
		}
		if (!dynamicQuery.getFood().equals("")) {
			criteria.add(Criteria.where("food").in(dynamicQuery.getFood()));
		}
		if (dynamicQuery.getLat() != null && dynamicQuery.getLng() != null && dynamicQuery.getRadius() != null) {
			criteria.add(
					Criteria.where("address.location")
					.nearSphere(new Point(dynamicQuery.getLat(), dynamicQuery.getLng())).maxDistance(dynamicQuery.getRadius()));
					//.withinSphere(new Circle(dynamicQuery.getLat(), dynamicQuery.getLng(), dynamicQuery.getRadius())));
		}
		if (!criteria.isEmpty()) {
			query.addCriteria(new Criteria().andOperator(criteria.toArray(new Criteria[criteria.size()])));
		}
		query.with(pageableRequest);
		List<Event> events = mongoTemplate.find(query, Event.class, "events");
		return PageableExecutionUtils.getPage(events, pageableRequest, () -> mongoTemplate.count(query, Event.class));
	}

	@Override
	public boolean checkForEventOverlap(String user, LocalDateTime eventStart, LocalDateTime eventFinish,
			boolean flag) {
		Query query = new Query();
		List<Criteria> criteria = new ArrayList<>();
		if (flag) {
			criteria.add(Criteria.where("owner").is(user));
		} else {
			criteria.add(Criteria.where("participants").in(user));
		}
		Criteria cr1 = new Criteria().andOperator(Criteria.where("dateTimeStart").lte(eventStart),
				Criteria.where("dateTimeFinish").gte(eventStart));
		Criteria cr2 = new Criteria().andOperator(Criteria.where("dateTimeStart").gte(eventStart),
				Criteria.where("dateTimeFinish").lte(eventFinish));
		Criteria cr3 = new Criteria().andOperator(Criteria.where("dateTimeStart").lte(eventFinish),
				Criteria.where("dateTimeFinish").gte(eventFinish));
		criteria.add(new Criteria().orOperator(cr1, cr2, cr3));
		query.addCriteria(new Criteria().andOperator(criteria.toArray(new Criteria[criteria.size()])));
		// System.out.println(query);
		long res = mongoTemplate.count(query, Event.class, "events");
		return res != 0;
	}

	@Override
	public void changeEventStatus(long eventId) {
		Query query = new Query();
		query.addCriteria(Criteria.where("_id").is(eventId));
		Update update = new Update().set("status", "pending");
		mongoTemplate.findAndModify(query, update, Event.class);
	}

	/*@Override
	public void updateOverLapingEvents(String user, LocalDateTime eventStart, LocalDateTime eventFinish) {
		Query query = new Query();
		List<Criteria> criteria = new ArrayList<>();
		criteria.add(Criteria.where("participants").in(user));
		Criteria cr1 = new Criteria().andOperator(Criteria.where("dateTimeStart").lte(eventStart),
				Criteria.where("dateTimeFinish").gte(eventFinish));
		Criteria cr2 = new Criteria().andOperator(Criteria.where("dateTimeStart").gte(eventStart),
				Criteria.where("dateTimeFinish").lte(eventFinish));
		Criteria cr3 = new Criteria().andOperator(Criteria.where("dateTimeStart").gte(eventStart),
				Criteria.where("dateTimeFinish").gte(eventFinish));
		Criteria cr4 = new Criteria().andOperator(Criteria.where("dateTimeStart").lte(eventStart),
				Criteria.where("dateTimeFinish").lte(eventFinish));
		criteria.add(new Criteria().orOperator(cr1, cr2, cr3, cr4));
		query.addCriteria(new Criteria().andOperator(criteria.toArray(new Criteria[criteria.size()])));
		Update update = new Update().pull("subscribers", user);
		mongoTemplate.updateMulti(query, update, Event.class);
		
	}
*/
}
