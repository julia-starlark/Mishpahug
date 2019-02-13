package mishpahug.service;

import java.security.Principal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.ExampleMatcher;
import org.springframework.data.domain.Sort;
import org.springframework.data.querydsl.QPageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import mishpahug.dao.ArchiveRepository;
import mishpahug.dao.EventsRepository;
import mishpahug.dao.UserAccountRepository;
import mishpahug.domain.Address;
import mishpahug.domain.Event;
import mishpahug.domain.EventArchive;
import mishpahug.domain.Location;
import mishpahug.domain.Notification;
import mishpahug.domain.User;
import mishpahug.dto.AddressDto;
import mishpahug.dto.CalendarResponseDto;
import mishpahug.dto.EventCreateDto;
import mishpahug.dto.EventDateTimeDto;
import mishpahug.dto.EventForCalendarDto;
import mishpahug.dto.EventResponseDto;
import mishpahug.dto.EventStatusResponseDto;
import mishpahug.dto.EventsHistoryListResponseDto;
import mishpahug.dto.EventsInProgressResponseDto;
import mishpahug.dto.EventsListResponseDto;
import mishpahug.dto.InviteResponseDto;
import mishpahug.dto.LocationDto;
import mishpahug.dto.OwnerDto;
import mishpahug.dto.ParticipantDto;
import mishpahug.dto.SuccessResponseDto;
import mishpahug.exceptions.ConflictException;
import mishpahug.exceptions.InvalidDataException;

@Service
public class EventServiceImpl implements EventService {

	@Autowired
	EventsRepository eventsRepository;

	@Autowired
	UserAccountRepository userRepository;

	@Autowired
	ArchiveRepository archiveRepository;

	@Override
	@Transactional
	public SuccessResponseDto addEvent(EventCreateDto eventCreateDto, Principal principal) {
		Location location = new Location(eventCreateDto.getAddress().getLocation().getLat(),
				eventCreateDto.getAddress().getLocation().getLng(), 0.);
						Address address = new Address(eventCreateDto.getAddress().getCity(), eventCreateDto.getAddress().getPlace_id(),
				location);
		Event event = new Event(eventCreateDto.getTitle(), eventCreateDto.getHoliday(), eventCreateDto.getConfession(),
				eventCreateDto.getDate(), eventCreateDto.getTime(), eventCreateDto.getDuration(), address,
				eventCreateDto.getFood(), eventCreateDto.getDescription(), principal.getName());
		/*if (event.getDate().isBefore(LocalDate.now().plusDays(2))) {
			throw new InvalidDataException("Invalid data!");
		}*/
		List<EventDateTimeDto> ownerEvents = eventsRepository.findEventDateTimeByOwner(event.getOwner());
		LocalDateTime eventStart = LocalDateTime.of(event.getDate(), event.getTime());
		LocalDateTime eventFinish = LocalDateTime.of(event.getDate(), event.getTime()).plusMinutes(event.getDuration());
		List<EventDateTimeDto> eventOverlap = ownerEvents.stream()
				.filter(e -> (LocalDateTime.of(e.getDate(), e.getTime()).isBefore(eventStart)
						&& LocalDateTime.of(e.getDate(), e.getTime()).plusMinutes(e.getDuration()).isAfter(eventStart))
						|| (LocalDateTime.of(e.getDate(), e.getTime()).isAfter(eventStart) && LocalDateTime
								.of(e.getDate(), e.getTime()).plusMinutes(e.getDuration()).isBefore(eventFinish)))
				.collect(Collectors.toList());
		if (!eventOverlap.isEmpty()) {
			throw new ConflictException("This user has already created the event on this date and time!");
		}
		eventsRepository.save(event);
		//checkForPendingStatus(event);
		changeEventStatusToDone(event);
		return new SuccessResponseDto("Event is created");

	}

	private void changeEventStatusToDone(Event event) {
		ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
		LocalDateTime eventFinish = LocalDateTime.of(event.getDate(), event.getTime()).plusMinutes(event.getDuration());
		Duration duration = Duration.between(LocalDateTime.now(), eventFinish);
		long delay = duration.toMinutes();
		scheduler.schedule(new Runnable() {
			@Override
			public void run() {
				if (!event.getStatus().equals("not done")) {
					event.setStatus("done");
				}
				archiveRepository.save(convertToEventArchive(event));
				eventsRepository.delete(event);
				List<User> participants = userRepository.findByInvitationsIn(event.getEventId());
				if (!participants.isEmpty()) {
					participants.forEach(u -> u.deleteInvitation(event.getEventId()));
					userRepository.saveAll(participants);
				}
			}

		}, delay, TimeUnit.MINUTES);
	}

	private void checkForPendingStatus(Event event) {
		ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
		LocalDateTime eventStart = LocalDateTime.of(event.getDate(), event.getTime());
		LocalDateTime changeToPending = eventStart.minusHours(24);
		Duration duration = Duration.between(LocalDateTime.now(), changeToPending);
		long delay = duration.toMinutes();
		scheduler.schedule(new Runnable() {
			@Override
			public void run() {
				if (!event.getStatus().equals("pending")) {
					event.setStatus("not done");
					String message = "We are sorry to inform you that the event " + event.getTitle()
							+ " that was suppossed to take place on " + event.getDate() + " was cancelled.";
					Notification notification = new Notification("Event Cancelation", message, event.getEventId());
					Set<String> eventSubscribers = event.getSubscribers();
					eventSubscribers.stream().map(s -> userRepository.findById(s).get()).forEach(u -> {
						u.addNotification(notification);
						userRepository.save(u);
					});
					archiveRepository.save(convertToEventArchive(event));
					eventsRepository.delete(event);
				}
			}
		}, delay, TimeUnit.MINUTES);
	}

	private EventArchive convertToEventArchive(Event event) {
		return EventArchive.builder().eventId(event.getEventId()).title(event.getTitle()).holiday(event.getHoliday())
				.confession(event.getConfession()).date(event.getDate()).time(event.getTime())
				.duration(event.getDuration()).address(event.getAddress()).food(event.getFood())
				.description(event.getDescription()).status(event.getStatus()).participants(event.getParticipants())
				.owner(event.getOwner()).build();
	}

	@Override
	// FIXME check web security (throw "User is not associated with the event!"
	// exception)
	public EventResponseDto getOwnEventInfoById(Long eventId) {
		Event event = eventsRepository.findById(eventId).orElse(null);
		Set<User> users = event.getSubscribers().stream().map(id -> userRepository.findById(id).get())
				.collect(Collectors.toSet());
		Set<ParticipantDto> participants = users.stream().map(u -> convertToParticipantDto(u, event))
				.collect(Collectors.toSet());
		if (event.getStatus().equals("in progress")) {
			participants.forEach(p -> p.setPhoneNumber(null));
		}
		if (event.getStatus().equals("pending")) {
			users = users.stream().filter(u -> u.getInvitations().contains(event.getEventId()))
					.collect(Collectors.toSet());
			participants = users.stream().map(u -> convertToParticipantDto(u, event)).collect(Collectors.toSet());
		}
		return convertToEventResponseDto(event, participants);
	}

	private EventResponseDto convertToEventResponseDto(Event event, Set<ParticipantDto> participants) {
		return EventResponseDto.builder().eventId(event.getEventId()).title(event.getTitle())
				.holiday(event.getHoliday()).confession(event.getConfession()).date(event.getDate())
				.time(event.getTime()).duration(event.getDuration()).food(event.getFood())
				.description(event.getDescription()).status(event.getStatus()).participants(participants).build();
	}

	private ParticipantDto convertToParticipantDto(User u, Event event) {
		return ParticipantDto.builder().userId(u.getUserId()).fullName(u.getFirstName() + u.getLastName())
				.confession(u.getConfession()).gender(u.getGender())
				.age(Period.between(u.getDateOfBirth(), LocalDate.now()).getYears()).pictureLink(u.getPictureLink())
				.maritalStatus(u.getMaritalStatus()).foodPreferences(u.getFoodPreferences()).languages(u.getLanguages())
				.rate(u.getRate()).phoneNumber(u.getPhoneNumber()).numberOfVoters(u.getNumberOfVoters())
				/* .isInvited(u.getInvitations().contains(event.getEventId()) ? false : true) */.build();
	}

	@Override
	// FIXME check web security (throw "User is not associated with the event!"
	// exception)
	public EventResponseDto getSubscribedEventById(Long eventId) {
		Event event = eventsRepository.findById(eventId).orElse(null);
		Address eventAddress = event.getAddress();
		User eventOwner = userRepository.findById(event.getOwner()).get();
		Location eventLocation = eventAddress.getLocation();
		LocationDto location = LocationDto.builder().lat(eventLocation.getLat()).lng(eventLocation.getLng()).build();
		AddressDto address = AddressDto.builder().city(eventAddress.getCity()).place_id(eventAddress.getPlace_id())
				.location(location).build();
		OwnerDto owner = convertToOwnerDto(eventOwner);
		if (event.getStatus().equals("in progress")) {
			address.setLocation(null);
			address.setPlace_id(null);
			owner.setPhoneNumber(null);
		}
		return convertToEventResponseDto(event, owner);
	}

	private OwnerDto convertToOwnerDto(User eventOwner) {
		return OwnerDto.builder().fullName(eventOwner.getFirstName() + eventOwner.getLastName())
				.confession(eventOwner.getConfession()).gender(eventOwner.getGender())
				.age(Period.between(eventOwner.getDateOfBirth(), LocalDate.now()).getYears())
				.pictureLink(eventOwner.getPictureLink()).phoneNumber(eventOwner.getPhoneNumber())
				.maritalStatus(eventOwner.getMaritalStatus()).foodPreferences(eventOwner.getFoodPreferences())
				.languages(eventOwner.getLanguages()).rate(eventOwner.getRate())
				.numberOfVoters(eventOwner.getNumberOfVoters()).build();
	}

	private EventResponseDto convertToEventResponseDto(Event event, OwnerDto owner) {
		Address eventAddress = event.getAddress();
		Location eventLocation = eventAddress.getLocation();
		LocationDto location = LocationDto.builder().lat(eventLocation.getLat()).lng(eventLocation.getLng()).build();
		AddressDto address = AddressDto.builder().city(eventAddress.getCity()).place_id(eventAddress.getPlace_id())
				.location(location).build();
		return EventResponseDto.builder().eventId(event.getEventId()).title(event.getTitle())
				.holiday(event.getHoliday()).confession(event.getConfession()).date(event.getDate())
				.time(event.getTime()).duration(event.getDuration()).address(address).food(event.getFood())
				.description(event.getDescription()).status(event.getStatus()).owner(owner).build();
	}

	@Override
	public CalendarResponseDto getEventsByMonth(int month, Principal principal) {
		int year = LocalDate.now().getYear();
		LocalDate dateFrom = LocalDate.of(year, month, 1);
		/*
		 * YearMonth yearMonth = YearMonth.of(LocalDate.now().getYear(), month); int
		 * daysInMonth = yearMonth.lengthOfMonth(); LocalDate dateTo =
		 * dateFrom.plusDays(daysInMonth).plusDays(1);
		 */
		if (month == 12) {
			month = 1;
			year = LocalDate.now().getYear() + 1;
		}
		LocalDate dateTo = LocalDate.of(year, month, 1);
		List<EventForCalendarDto> eventsList = eventsRepository.findEventByMonth(dateFrom, dateTo, principal.getName());
		Set<EventForCalendarDto> myEvents = new HashSet<>();
		Set<EventForCalendarDto> subscribedEvents = new HashSet<>();
		eventsList.sort((e1, e2) -> e1.getDate().compareTo(e2.getDate()));
		eventsList.forEach(e -> {
			String owner = e.getOwner();
			e.setOwner(null);
			if (owner.equals(principal.getName())) {
				myEvents.add(e);
				return;
			} else {
				subscribedEvents.add(e);
				return;
			}
		});
		return new CalendarResponseDto(myEvents, subscribedEvents);
	}

	@Override
	public EventsListResponseDto getOwnEventsList(Principal principal) {
		List<Event> myEvents = eventsRepository.findEventByOwner(principal.getName());
		List<EventResponseDto> eventsRespDto = new ArrayList<>();
		for (Event e : myEvents) {
			Set<ParticipantDto> participants = e.getParticipants().stream().map(id -> userRepository.findById(id).get())
					.map(u -> convertToParticipantDto(u, e)).collect(Collectors.toSet());
			if (e.getStatus().equals("in progress")) {
				participants.forEach(p -> p.setPhoneNumber(null));
			}
			eventsRespDto.add(convertToEventResponseDto(e, participants));
		}
		EventsListResponseDto events = new EventsListResponseDto(eventsRespDto);
		return events;
	}

	@Override
	public EventsHistoryListResponseDto getOwnDoneEventsList(Principal principal) {
		List<EventResponseDto> ownEvents = archiveRepository.findByOwner(principal.getName());
		ownEvents.sort(new EventsDateDescendingComparator());
		EventsHistoryListResponseDto events = new EventsHistoryListResponseDto(ownEvents);
		return events;
	}

	@Override
	public EventsListResponseDto getSubscribedEvents(Principal principal) {
		String userLogin = principal.getName();
		List<Event> eventsArchive = archiveRepository.findEventsByParticipants(userLogin);
		List<EventResponseDto> subcribedEvents = new ArrayList<>();
		if (!eventsArchive.isEmpty()) {
			eventsArchive.stream().map(
					e -> convertToEventResponseDto(e, convertToOwnerDto(userRepository.findById(e.getOwner()).get())))
					.forEach(e -> {
						e.getOwner().setPhoneNumber(null);
						e.setTime(null);
						e.setDuration(null);
						e.setAddress(null);
						e.setFood(null);
						subcribedEvents.add(e);
					});
			/*
			 * for (Event e : eventsArchive) { User owner =
			 * userRepository.findById(e.getOwner()).get(); EventResponseDto eventDto =
			 * convertToEventResponseDto(e, convertToOwnerDto(owner));
			 * eventDto.getOwner().setPhoneNumber(null); eventDto.setTime(null);
			 * eventDto.setDuration(null); eventDto.setAddress(null);
			 * eventDto.setFood(null); subcribedEvents.add(eventDto); }
			 */
		}
		List<Event> events = eventsRepository.findEventBySubscribers(userLogin);
		if (!events.isEmpty()) {
			events.stream().map(
					e -> convertToEventResponseDto(e, convertToOwnerDto(userRepository.findById(e.getOwner()).get())))
					.forEach(e -> {
						if (e.getStatus().equals("in progress")) {
							e.getOwner().setPhoneNumber(null);
							e.getAddress().setLocation(null);
							e.getAddress().setPlace_id(null);
						}
						subcribedEvents.add(e);
					});
			/*
			 * for (Event e : events) { User owner =
			 * userRepository.findById(e.getOwner()).get(); OwnerDto ownerDto =
			 * convertToOwnerDto(owner); EventResponseDto eventDto =
			 * convertToEventResponseDto(e, ownerDto); if
			 * (e.getStatus().equals("in progress")) {
			 * eventDto.getOwner().setPhoneNumber(null);
			 * eventDto.getAddress().setLocation(null);
			 * eventDto.getAddress().setPlace_id(null); } subcribedEvents.add(eventDto); }
			 */
		}
		EventsListResponseDto eventsList = new EventsListResponseDto(subcribedEvents);
		return eventsList;
	}

	private class EventsDateDescendingComparator implements Comparator<EventResponseDto> {

		@Override
		public int compare(EventResponseDto e1, EventResponseDto e2) {
			int res = e2.getDate().compareTo(e1.getDate());
			if (res == 0) {
				return e2.getTime().compareTo(e1.getTime());
			}
			return res;
		}

	}

	@Override
	@Transactional
	public SuccessResponseDto subscribeToEvent(Long eventId, Principal principal) {
		Event event = eventsRepository.findById(eventId).orElse(null);
		String userLogin = principal.getName();
		if (event.getOwner().equals(userLogin) || event.getSubscribers().contains(userLogin)
				|| !eventsRepository
						.findOverlapByDate(userRepository.findById(userLogin).get().getInvitations(), event.getDate())
						.isEmpty()) {
			throw new ConflictException("User is the owner of the event or already subscribed to it!");
		}
		event.addSubscriber(principal.getName());
		User owner = userRepository.findById(event.getOwner()).get();
		String message = "User " + userRepository.findById(principal.getName()).get().getUserId()
				+ " subscribed to your event " + event.getTitle() + " that is suppossed to take place on "
				+ event.getDate() + ".";
		Notification notification = new Notification("Subscription to event", message, eventId);
		owner.addNotification(notification);
		userRepository.save(owner);
		eventsRepository.save(event);
		return new SuccessResponseDto("User subscribed to the event!");
	}

	@Override
	@Transactional
	public SuccessResponseDto unsubscribeFromEvent(Long eventId, Principal principal) {
		// FIXME security check if user is subscribed for the event
		Event event = eventsRepository.findById(eventId).orElse(null);
		if (!event.getStatus().equals("in progress")) {
			throw new ConflictException("User can't unsubscribe from the event!");
		}
		event.deleteParticipant(principal.getName());
		User owner = userRepository.findById(event.getOwner()).get();
		String message = "User " + userRepository.findById(principal.getName()).get().getUserId()
				+ " unsubscribed from your event " + event.getTitle() + " that is suppossed to take place on "
				+ event.getDate() + ".";
		Notification notification = new Notification("Unsubscription from event", message, eventId);
		owner.addNotification(notification);
		userRepository.save(owner);
		eventsRepository.save(event);
		return new SuccessResponseDto("User unsubscribed from the event!");
	}

	@Override
	@Transactional
	public SuccessResponseDto voteForEvent(Long eventId, Double voteCount, Principal principal) {
		// FIXME web security check if user was a participant of the event
		Event event = eventsRepository.findById(eventId).orElse(null);
		String userLogin = principal.getName();
		User participant = userRepository.findById(userLogin).get();
		User owner = userRepository.findById(event.getOwner()).get();
		double currentRate = owner.getRate();
		int numVoters = owner.getNumberOfVoters();
		double newRate = (currentRate * numVoters + voteCount) / ++numVoters;
		owner.setRate(newRate);
		owner.setNumberOfVoters(numVoters++);
		String message = participant.getFirstName() + " " + participant.getLastName() + " has voted for the event "
				+ event.getTitle();
		Notification notification = new Notification("Vote for Event", message, eventId);
		owner.addNotification(notification);
		userRepository.save(owner);
		event.getVoted().add(userLogin);
		eventsRepository.save(event);
		return new SuccessResponseDto("User vote is accepted!");
	}

	@Override
	@Transactional
	public InviteResponseDto inviteToEvent(Long eventId, Long userId) {
		// FIXME check that the user is the owner of the event
		// FIXME remove from subscribers and put to the participants
		Event event = eventsRepository.findById(eventId).orElse(null);
		User user = userRepository.findUserByUserId(userId);
		String login = user.getLogin();
		if (!event.getParticipants().contains(login) || user.getInvitations().contains(userId)) {
			throw new ConflictException("User is already invited to the event or is not subscribed to the event!");
		}
		user.addInvitation(eventId);
		List<Event> eventsOverlap = eventsRepository.findDateOverlapForUser(login, event.getDate());
		if (!eventsOverlap.isEmpty()) {
			for (Event e : eventsOverlap) {
				e.deleteParticipant(login);
				eventsRepository.save(e);
			}
		}
		String message = "You were invited to the event " + event.getTitle() + " that takes place on "
				+ event.getDate();
		Notification notification = new Notification("Invitation", message, eventId);
		user.addNotification(notification);
		userRepository.save(user);
		return new InviteResponseDto(userId, true);
	}

	@Override
	@Transactional
	public EventStatusResponseDto changeEventStatus(Long eventId) {
		// FIXME security check that the user is the owner of the event
		Event event = eventsRepository.findById(eventId).orElse(null);
		event.setStatus("pending");
		eventsRepository.save(event);
		return new EventStatusResponseDto(eventId, "Pending");
	}

	@Override
	public EventsInProgressResponseDto getAllEventsInProgress(int page, int size) {
		Event event = new Event();
		event.setStatus("in progress");
		ExampleMatcher matcher = ExampleMatcher.matching().withIgnorePaths("eventId", "title", "holiday", "confession",
				"date", "time", "duration", "address", "food", "description", "subscribers", "participants", "voted",
				"owner");
		Example<Event> example = Example.of(event, matcher);
		QPageRequest pageRequest = new QPageRequest(page, size);
		List<Event> content = eventsRepository.findAll(example, pageRequest).getContent();
		List<EventResponseDto> events = content.stream()
				.map(e -> convertToEventResponseDto(e, convertToOwnerDto(userRepository.findById(e.getOwner()).get())))
				.collect(Collectors.toList());
		events.forEach(e -> {
			e.getAddress().setLocation(null);
			e.getAddress().setPlace_id(null);
			e.setStatus(null);
			e.setParticipants(null);
			e.getOwner().setPhoneNumber(null);
		});
		int totalElements = content.size();
		int totalPages = totalElements % size != 0 ? totalElements / size + 1 : totalElements / size;
		int number = pageRequest.getPageNumber();
		boolean first = number == 0;
		boolean last = number + 1 == totalPages;
		int numberOfElements = last
				? (totalElements % totalPages == 0 ? totalElements / totalPages : totalElements % totalPages)
				: totalElements / totalPages;
		Sort sort = pageRequest.getSort();
		EventsInProgressResponseDto res = new EventsInProgressResponseDto(events, totalElements, totalPages, size,
				number, numberOfElements, first, last, sort);
		return res;
	}

}
