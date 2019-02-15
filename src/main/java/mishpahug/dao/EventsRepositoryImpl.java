package mishpahug.dao;

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
		if (dynamicQuery.getDateFrom() != null) {
			criteria.add(Criteria.where("date").gte(dynamicQuery.getDateFrom()));
		}
		if (dynamicQuery.getDateTo() != null) {
			criteria.add(Criteria.where("date").lte(dynamicQuery.getDateTo()));
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
					Criteria.where("address.location").near(new Point(dynamicQuery.getLat(), dynamicQuery.getLng()))
							.minDistance(0).maxDistance(dynamicQuery.getRadius()));
		}
		if (!criteria.isEmpty()) {
			query.addCriteria(new Criteria().andOperator(criteria.toArray(new Criteria[criteria.size()])));
		}
		query.with(pageableRequest);
		List<Event> events = mongoTemplate.find(query, Event.class, "events");
		return PageableExecutionUtils.getPage(events, pageableRequest, () -> mongoTemplate.count(query, Event.class));
	}

}
