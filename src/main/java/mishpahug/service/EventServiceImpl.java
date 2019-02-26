package mishpahug.service;

import java.security.Principal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import mishpahug.dao.ArchiveRepository;
import mishpahug.dao.DynamicQuery;
import mishpahug.dao.EventsRepository;
import mishpahug.dao.UserAccountRepository;
import mishpahug.domain.Address;
import mishpahug.domain.Event;
import mishpahug.domain.EventArchive;
import mishpahug.domain.Notification;
import mishpahug.domain.User;
import mishpahug.dto.AddressDto;
import mishpahug.dto.CalendarResponseDto;
import mishpahug.dto.EventCreateDto;
import mishpahug.dto.EventForCalendarDto;
import mishpahug.dto.EventResponseDto;
import mishpahug.dto.EventStatusResponseDto;
import mishpahug.dto.EventsHistoryListResponseDto;
import mishpahug.dto.EventsInProgressResponseDto;
import mishpahug.dto.EventsListResponseDto;
import mishpahug.dto.FiltersDto;
import mishpahug.dto.InviteResponseDto;
import mishpahug.dto.LocationDto;
import mishpahug.dto.NotificationNewDto;
import mishpahug.dto.NotificationTitle;
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

	@Autowired
	NotificationFactory notificationFactory;

	@Override
	@Transactional
	public SuccessResponseDto addEvent(EventCreateDto eventCreateDto, Principal principal) {
		String owner = principal.getName();
		double[] location = { eventCreateDto.getAddress().getLocation().getLat(),
				eventCreateDto.getAddress().getLocation().getLng() };
		Address address = new Address(eventCreateDto.getAddress().getCity(), eventCreateDto.getAddress().getPlace_id(),
				location);
		Event event = new Event(eventCreateDto.getTitle(), eventCreateDto.getHoliday(), eventCreateDto.getConfession(),
				eventCreateDto.getDate(), eventCreateDto.getTime(), eventCreateDto.getDuration(), address,
				eventCreateDto.getFood(), eventCreateDto.getDescription(), owner);
		if (event.getDateTimeStart().isBefore(LocalDateTime.now().plusDays(2))) {
			throw new InvalidDataException("Invalid data!");
		}
		if (eventsRepository.checkForEventOverlap(owner, event.getDateTimeStart(), event.getDateTimeFinish(), true)) {
			throw new ConflictException("This user has already created the event on this date and time!");
		}
		eventsRepository.save(event);
		checkForPendingStatus(event.getEventId(), event.getDateTimeStart());
		changeEventStatusToDone(event.getEventId(), event.getDateTimeFinish());
		return new SuccessResponseDto("Event is created");

	}

	private void changeEventStatusToDone(long eventId, LocalDateTime eventFinish) {
		ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
		Duration duration = Duration.between(LocalDateTime.now(), eventFinish);
		long delay = duration.toMinutes();
		scheduler.schedule(new Runnable() {
			@Override
			public void run() {
				Event event = eventsRepository.findById(eventId).get();
				if (!event.getStatus().equals("not done")) {
					event.setStatus("done");
				}
				Set<String> particip = event.getParticipants();
				archiveRepository.save(convertToEventArchive(event));
				eventsRepository.delete(event);
				List<User> participants = userRepository.findParticipants(particip, eventId);
				if (!participants.isEmpty()) {
					participants.forEach(u -> u.deleteInvitation(event.getEventId()));
					participants.forEach(u -> u.addNotification(notificationFactory.creteNewNotification(
							NotificationNewDto.builder().eventId(eventId).date(event.getDateTimeStart())
									.title(NotificationTitle.EVENT_CANCELATION).eventTitle(event.getTitle()).build())));
					userRepository.saveAll(participants);
				}
			}

		}, delay, TimeUnit.MINUTES);
	}

	private void checkForPendingStatus(long eventId, LocalDateTime eventStart) {
		ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
		LocalDateTime changeToPending = eventStart.minusHours(24);
		Duration duration = Duration.between(LocalDateTime.now(), changeToPending);
		long delay = duration.toMinutes();
		scheduler.schedule(new Runnable() {
			@Override
			public void run() {
				Event event = eventsRepository.findById(eventId).get();
				if (!event.getStatus().equals("pending")) {
					event.setStatus("not done");
					Notification notification = notificationFactory.creteNewNotification(NotificationNewDto.builder()
							.title(NotificationTitle.EVENT_CANCELATION).eventId(event.getEventId())
							.eventTitle(event.getTitle()).date(event.getDateTimeStart()).build());
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
				.confession(event.getConfession()).dateTimeStart(event.getDateTimeStart())
				.dateTimeFinish(event.getDateTimeFinish()).duration(event.getDuration()).address(event.getAddress())
				.food(event.getFood()).description(event.getDescription()).status(event.getStatus())
				.participants(event.getParticipants()).subscribers(event.getSubscribers()).voted(event.getVoted())
				.owner(event.getOwner()).build();
	}

	@Override
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
		LocalDateTime eventstart = event.getDateTimeStart();
		return EventResponseDto.builder().eventId(event.getEventId()).title(event.getTitle())
				.holiday(event.getHoliday()).confession(event.getConfession())
				.date(LocalDate.of(eventstart.getYear(), eventstart.getMonth(), eventstart.getDayOfMonth()))
				.time(LocalTime.of(eventstart.getHour(), eventstart.getMinute())).duration(event.getDuration())
				.food(event.getFood()).description(event.getDescription()).status(event.getStatus())
				.participants(participants).build();
	}

	private ParticipantDto convertToParticipantDto(User u, Event event) {
		return ParticipantDto.builder().userId(u.getUserId()).fullName(u.getFirstName() + " " + u.getLastName())
				.confession(u.getConfession()).gender(u.getGender())
				.age(Period.between(u.getDateOfBirth(), LocalDate.now()).getYears()).pictureLink(u.getPictureLink())
				.maritalStatus(u.getMaritalStatus()).foodPreferences(u.getFoodPreferences()).languages(u.getLanguages())
				.rate(u.getRate()).phoneNumber(u.getPhoneNumber()).numberOfVoters(u.getNumberOfVoters())
				/* .isInvited(u.getInvitations().contains(event.getEventId()) ? false : true) */.build();
	}

	@Override
	public EventResponseDto getSubscribedEventById(Long eventId) {
		Event event = eventsRepository.findById(eventId).orElse(null);
		Address eventAddress = event.getAddress();
		User eventOwner = userRepository.findById(event.getOwner()).get();
		// Location eventLocation = eventAddress.getLocation();
		double[] eventLocation = eventAddress.getLocation();
		LocationDto location = LocationDto.builder().lat(eventLocation[0]).lng(eventLocation[1]).build();
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
		return OwnerDto.builder().fullName(eventOwner.getFirstName() + " " + eventOwner.getLastName())
				.confession(eventOwner.getConfession()).gender(eventOwner.getGender())
				.age(Period.between(eventOwner.getDateOfBirth(), LocalDate.now()).getYears())
				.pictureLink(eventOwner.getPictureLink()).phoneNumber(eventOwner.getPhoneNumber())
				.maritalStatus(eventOwner.getMaritalStatus()).foodPreferences(eventOwner.getFoodPreferences())
				.languages(eventOwner.getLanguages()).rate(eventOwner.getRate())
				.numberOfVoters(eventOwner.getNumberOfVoters()).build();
	}

	private EventResponseDto convertToEventResponseDto(Event event, OwnerDto owner) {
		Address eventAddress = event.getAddress();
		// Location eventLocation = eventAddress.getLocation();
		double[] eventLocation = eventAddress.getLocation();
		LocationDto location = LocationDto.builder().lat(eventLocation[0]).lng(eventLocation[1]).build();
		AddressDto address = AddressDto.builder().city(eventAddress.getCity()).place_id(eventAddress.getPlace_id())
				.location(location).build();
		LocalDateTime eventstart = event.getDateTimeStart();
		return EventResponseDto.builder().eventId(event.getEventId()).title(event.getTitle())
				.holiday(event.getHoliday()).confession(event.getConfession())
				.date(LocalDate.of(eventstart.getYear(), eventstart.getMonth(), eventstart.getDayOfMonth()))
				.time(LocalTime.of(eventstart.getHour(), eventstart.getMinute())).duration(event.getDuration())
				.duration(event.getDuration()).address(address).food(event.getFood())
				.description(event.getDescription()).status(event.getStatus()).owner(owner).build();
	}

	@Override
	public CalendarResponseDto getEventsByMonth(int month, Principal principal) {
		if (month < 1 || month > 12) {
			throw new InvalidDataException();
		}
		int year = LocalDate.now().getYear();
		LocalDateTime dateFrom = LocalDateTime.of(year, month++, 1, 0, 0, 0);
		if (month - 1 == 12) {
			month = 1;
			year = LocalDate.now().getYear() + 1;
		}
		LocalDateTime dateTo = LocalDateTime.of(year, month, 1, 0, 0, 0);
		List<EventForCalendarDto> eventsList = eventsRepository.findEventByMonth(dateFrom, dateTo, principal.getName())
				.stream().map(e -> convertToEventForCalendarDto(e)).collect(Collectors.toList());
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

	private EventForCalendarDto convertToEventForCalendarDto(Event e) {
		LocalDateTime date = e.getDateTimeStart();
		return EventForCalendarDto.builder().eventId(e.getEventId()).title(e.getTitle())
				.date(LocalDate.of(date.getYear(), date.getMonth(), date.getDayOfMonth()))
				.time(LocalTime.of(date.getHour(), date.getMinute())).duration(e.getDuration()).status(e.getStatus())
				.owner(e.getOwner()).build();
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
		List<Event> myEvents = archiveRepository.findByOwner(principal.getName());
		List<EventResponseDto> ownEvents = myEvents.stream()
				.map(e -> EventResponseDto.builder().eventId(e.getEventId()).title(e.getTitle()).holiday(e.getHoliday())
						.confession(e.getConfession())
						.date(LocalDate.of(e.getDateTimeStart().getYear(), e.getDateTimeStart().getMonth(),
								e.getDateTimeStart().getDayOfMonth()))
						.food(e.getFood()).description(e.getDescription()).status(e.getStatus()).build())
				.sorted((e1, e2) -> e2.getDate().compareTo(e1.getDate())).collect(Collectors.toList());
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
		}
		Collections.sort(subcribedEvents);
		EventsListResponseDto eventsList = new EventsListResponseDto(subcribedEvents);
		return eventsList;
	}

	@Override
	@Transactional
	public SuccessResponseDto subscribeToEvent(Long eventId, Principal principal) {
		Event event = eventsRepository.findById(eventId).orElse(null);
		String userLogin = principal.getName();
		if (event.getOwner().equals(userLogin) || event.getSubscribers().contains(userLogin) || eventsRepository
				.checkForEventOverlap(userLogin, event.getDateTimeStart(), event.getDateTimeFinish(), false)) {
			throw new ConflictException("User is the owner of the event or already subscribed to it!");
		}
		event.addSubscriber(principal.getName());
		User owner = userRepository.findById(event.getOwner()).get();
		User user = userRepository.findById(principal.getName()).get();
		Notification notification = notificationFactory
				.creteNewNotification(NotificationNewDto.builder().title(NotificationTitle.SUBSCRIPTION_TO_EVENT)
						.eventId(event.getEventId()).eventTitle(event.getTitle()).date(event.getDateTimeStart())
						.userFirstName(user.getFirstName()).userLastName(user.getLastName()).build());
		owner.addNotification(notification);
		userRepository.save(owner);
		eventsRepository.save(event);
		return new SuccessResponseDto("User subscribed to the event!");
	}

	@Override
	@Transactional
	public SuccessResponseDto unsubscribeFromEvent(Long eventId, Principal principal) {
		Event event = eventsRepository.findById(eventId).orElse(null);
		if (!event.getStatus().equals("in progress")) {
			throw new ConflictException("User can't unsubscribe from the event!");
		}
		event.deleteSubscriber(principal.getName());
		User owner = userRepository.findById(event.getOwner()).get();
		User user = userRepository.findById(principal.getName()).get();
		Notification notification = notificationFactory
				.creteNewNotification(NotificationNewDto.builder().title(NotificationTitle.UNSUBSCRIPTION_FROM_EVENT)
						.eventId(event.getEventId()).eventTitle(event.getTitle()).date(event.getDateTimeStart())
						.userFirstName(user.getFirstName()).userLastName(user.getLastName()).build());
		owner.addNotification(notification);
		userRepository.save(owner);
		eventsRepository.save(event);
		return new SuccessResponseDto("User unsubscribed from the event!");
	}

	@Override
	@Transactional
	public SuccessResponseDto voteForEvent(Long eventId, Double voteCount, Principal principal) {
		EventArchive event = archiveRepository.findById(eventId).orElse(null);
		String userLogin = principal.getName();
		User participant = userRepository.findById(userLogin).get();
		User owner = userRepository.findById(event.getOwner()).get();
		double currentRate = owner.getRate();
		int numVoters = owner.getNumberOfVoters();
		double newRate = (currentRate * numVoters + voteCount) / ++numVoters;
		owner.setRate(newRate);
		owner.setNumberOfVoters(numVoters++);
		Notification notification = notificationFactory
				.creteNewNotification(NotificationNewDto.builder().title(NotificationTitle.NEW_VOTE)
						.eventTitle(event.getTitle()).date(event.getDateTimeStart()).eventId(event.getEventId())
						.userFirstName(participant.getFirstName()).userLastName(participant.getLastName()).build());
		owner.addNotification(notification);
		userRepository.save(owner);
		event.getVoted().add(userLogin);
		archiveRepository.save(event);
		return new SuccessResponseDto("User vote is accepted!");
	}

	@Override
	@Transactional
	public InviteResponseDto inviteToEvent(Long eventId, Long userId) {
		Event event = eventsRepository.findById(eventId).orElse(null);
		User user = userRepository.findUserByUserId(userId);
		String login = user.getLogin();
		if (!event.getSubscribers().contains(login) || user.getInvitations().contains(userId)) {
			throw new ConflictException("User is already invited to the event or is not subscribed to the event!");
		}
		user.addInvitation(eventId);
		event.addParticipant(login);
		LocalDateTime timeOccupiedfrom = event.getDateTimeStart().withHour(0).withMinute(0).withSecond(0);
		LocalDateTime timeOccupiedto = event.getDateTimeStart().withHour(23).withMinute(59).withSecond(59);
		List<Event> eventsOverlap = eventsRepository.findOverlapingEvents(login, timeOccupiedfrom, timeOccupiedto);
		eventsOverlap.forEach(e -> System.out.println(e));
		if (!eventsOverlap.isEmpty()) {
			for (Event e : eventsOverlap) {
				e.deleteSubscriber(login);
				eventsRepository.save(e);
			}
		}
		String message = "You were invited to the event " + event.getTitle() + " that takes place on "
				+ event.getDateTimeStart().format(DateTimeFormatter.ISO_LOCAL_DATE);
		Notification notification = new Notification("Invitation", message, eventId);
		user.addNotification(notification);
		userRepository.save(user);
		eventsRepository.save(event);
		return new InviteResponseDto(userId, true);
	}

	@Override
	@Transactional
	public EventStatusResponseDto changeEventStatus(Long eventId) {
		Event event = eventsRepository.findById(eventId).orElse(null);
		event.setStatus("pending");
		eventsRepository.save(event);
		return new EventStatusResponseDto(eventId, "Pending");
	}

	@Override
	public EventsInProgressResponseDto getAllEventsInProgress(int page, int size, FiltersDto filters) {
		Page<Event> eventsInProg = eventsRepository.query(DynamicQuery.builder()
				.confession(filters.getFilters().getConfession()).dateFrom(filters.getFilters().getDateFrom())
				.dateTo(filters.getFilters().getDateTo()).holiday(filters.getFilters().getHolidays())
				.food(filters.getFilters().getFood()).lat(filters.getLocation().getLat())
				.lng(filters.getLocation().getLng()).radius(filters.getLocation().getRadius()).build(), page, size);
		List<EventResponseDto> events = eventsInProg.getContent().stream()
				.map(e -> convertToEventResponseDto(e, convertToOwnerDto(userRepository.findById(e.getOwner()).get())))
				.sorted().collect(Collectors.toList());
		events.forEach(e -> {
			e.getAddress().setLocation(null);
			e.getAddress().setPlace_id(null);
			e.setStatus(null);
			e.setParticipants(null);
			e.getOwner().setPhoneNumber(null);
		});
		long totalElements = eventsInProg.getTotalElements();
		int totalPages = eventsInProg.getTotalPages();
		int number = eventsInProg.getNumber();
		boolean first = eventsInProg.isFirst();
		boolean last = eventsInProg.isLast();
		int numberOfElements = eventsInProg.getNumberOfElements();
		Sort sort = eventsInProg.getSort();
		EventsInProgressResponseDto res = new EventsInProgressResponseDto(events, totalElements, totalPages, size,
				number, numberOfElements, first, last, sort);
		return res;
	}

}
