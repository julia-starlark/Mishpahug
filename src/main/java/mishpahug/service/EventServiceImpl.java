package mishpahug.service;

import java.security.Principal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
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
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
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
	
	@Autowired
	DtoFactory dtoFactory;

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
				archiveRepository.save(dtoFactory.convertToEventArchive(event));
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
					archiveRepository.save(dtoFactory.convertToEventArchive(event));
					eventsRepository.delete(event);
				}
			}
		}, delay, TimeUnit.MINUTES);
	}

	@Override
	public EventResponseDto getOwnEventInfoById(Long eventId) {
		Event event = eventsRepository.findById(eventId).orElse(null);
		Set<User> users = event.getSubscribers().stream().map(id -> userRepository.findById(id).get())
				.collect(Collectors.toSet());
		Set<ParticipantDto> participants = users.stream().map(u -> dtoFactory.convertToParticipantDto(u, event))
				.collect(Collectors.toSet());
		if (event.getStatus().equals("in progress")) {
			participants.forEach(p -> p.setPhoneNumber(null));
		}
		if (event.getStatus().equals("pending")) {
			users = users.stream().filter(u -> u.getInvitations().contains(event.getEventId()))
					.collect(Collectors.toSet());
			participants = users.stream().map(u -> dtoFactory.convertToParticipantDto(u, event)).collect(Collectors.toSet());
		}
		return dtoFactory.convertToEventResponseDto(event, participants);
	}

	@Override
	public EventResponseDto getSubscribedEventById(Long eventId) {
		Event event = eventsRepository.findById(eventId).orElse(null);
		Address eventAddress = event.getAddress();
		User eventOwner = userRepository.findById(event.getOwner()).get();
		double[] eventLocation = eventAddress.getLocation();
		LocationDto location = LocationDto.builder().lat(eventLocation[0]).lng(eventLocation[1]).build();
		AddressDto address = AddressDto.builder().city(eventAddress.getCity()).place_id(eventAddress.getPlace_id())
				.location(location).build();
		OwnerDto owner = dtoFactory.convertToOwnerDto(eventOwner);
		if (event.getStatus().equals("in progress")) {
			address.setLocation(null);
			address.setPlace_id(null);
			owner.setPhoneNumber(null);
		}
		return dtoFactory.convertToEventResponseDto(event, owner);
	}

	@Override
	public CalendarResponseDto getEventsByMonth(int month, int year, Principal principal) {
		if (month < 1 || month > 12) {
			throw new InvalidDataException();
		}
		LocalDateTime dateFrom = LocalDateTime.of(year, month++, 1, 0, 0, 0);
		if (month - 1 == 12) {
			month = 1;
			year += 1;
		}
		LocalDateTime dateTo = LocalDateTime.of(year, month, 1, 0, 0, 0);
		List<EventForCalendarDto> eventsList = eventsRepository.findEventByMonth(dateFrom, dateTo, principal.getName())
				.stream().map(e -> dtoFactory.convertToEventForCalendarDto(e)).collect(Collectors.toList());
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
			Set<ParticipantDto> subscribers = e.getSubscribers().stream().map(id -> userRepository.findById(id).get())
					.map(u -> dtoFactory.convertToParticipantDto(u, e)).collect(Collectors.toSet());
			if (e.getStatus().equals("in progress")) {
				subscribers.forEach(p -> p.setPhoneNumber(null));
			}
			eventsRespDto.add(dtoFactory.convertToEventResponseDto(e, subscribers));
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
					e -> dtoFactory.convertToEventResponseDto(e, dtoFactory.convertToOwnerDto(userRepository.findById(e.getOwner()).get())))
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
					e -> dtoFactory.convertToEventResponseDto(e, dtoFactory.convertToOwnerDto(userRepository.findById(e.getOwner()).get())))
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
		Notification notification = notificationFactory
				.creteNewNotification(NotificationNewDto.builder().title(NotificationTitle.SUBSCRIPTION_TO_EVENT)
						.eventId(event.getEventId()).eventTitle(event.getTitle()).date(event.getDateTimeStart())
						.userFullName(userRepository.getUserFullName(principal.getName())).build());
		userRepository.addNotificationToUser(event.getOwner(), notification);
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
		Notification notification = notificationFactory
				.creteNewNotification(NotificationNewDto.builder().title(NotificationTitle.UNSUBSCRIPTION_FROM_EVENT)
						.eventId(event.getEventId()).eventTitle(event.getTitle()).date(event.getDateTimeStart())
						.userFullName(userRepository.getUserFullName(principal.getName())).build());
		userRepository.addNotificationToUser(event.getOwner(), notification);
		eventsRepository.save(event);
		return new SuccessResponseDto("User unsubscribed from the event!");
	}

	@Override
	@Transactional
	public SuccessResponseDto voteForEvent(Long eventId, Double voteCount, Principal principal) {
		//FIXME don't get whole event
		EventArchive event = archiveRepository.findById(eventId).orElse(null);
		String userLogin = principal.getName();
		User owner = userRepository.findById(event.getOwner()).get();
		double currentRate = owner.getRate();
		int numVoters = owner.getNumberOfVoters();
		double newRate = (currentRate * numVoters + voteCount) / ++numVoters;
		owner.setRate(newRate);
		owner.setNumberOfVoters(numVoters++);
		Notification notification = notificationFactory
				.creteNewNotification(NotificationNewDto.builder().title(NotificationTitle.NEW_VOTE)
						.eventTitle(event.getTitle()).date(event.getDateTimeStart()).eventId(event.getEventId())
						.userFullName(userRepository.getUserFullName(userLogin)).build());
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
		if (!event.getSubscribers().contains(login) || user.getInvitations().contains(eventId)) {
			throw new ConflictException("User is already invited to the event or is not subscribed to the event!");
		}
		user.addInvitation(eventId);
		event.addParticipant(login);
		LocalDateTime timeOccupiedfrom = event.getDateTimeStart().withHour(0).withMinute(0).withSecond(0);
		LocalDateTime timeOccupiedto = event.getDateTimeStart().withHour(23).withMinute(59).withSecond(59);
		List<Event> eventsOverlap = eventsRepository.findOverlapingEvents(login, timeOccupiedfrom, timeOccupiedto);
		if (!eventsOverlap.isEmpty()) {
			for (Event e : eventsOverlap) {
				e.deleteSubscriber(login);
				eventsRepository.save(e);
			}
		}
		user.addNotification(notificationFactory.creteNewNotification(NotificationNewDto.builder().title(NotificationTitle.INVITATION).eventId(eventId)
				.date(event.getDateTimeStart()).build()));
		userRepository.save(user);
		eventsRepository.save(event);
		return new InviteResponseDto(userId, true);
	}

	@Override
	@Transactional
	public EventStatusResponseDto changeEventStatus(Long eventId) {
		eventsRepository.changeEventStatus(eventId);
		return new EventStatusResponseDto(eventId, "Pending");
	}

	@Override
	public EventsInProgressResponseDto getAllEventsInProgress(int pageNum, int size, FiltersDto filters) {
		Pageable pageable = PageRequest.of(pageNum, size);
		
		List<Event> eventsInProg = eventsRepository.query(DynamicQuery.builder()
				.confession(filters.getFilters().getConfession()).dateFrom(filters.getFilters().getDateFrom())
				.dateTo(filters.getFilters().getDateTo()).holiday(filters.getFilters().getHolidays())
				.food(filters.getFilters().getFood()).lat(filters.getLocation().getLat())
				.lng(filters.getLocation().getLng()).radius(filters.getLocation().getRadius()).build(), pageNum, size);
		List<EventResponseDto> events = eventsInProg.stream()
				.map(e -> dtoFactory.convertToEventResponseDto(e, dtoFactory.convertToOwnerDto(userRepository.findById(e.getOwner()).get())))
				.sorted().collect(Collectors.toList());
		events.forEach(e -> {
			e.getAddress().setLocation(null);
			e.getAddress().setPlace_id(null);
			e.setStatus(null);
			e.setParticipants(null);
			e.getOwner().setPhoneNumber(null);
		});
		Page<EventResponseDto> page = new PageImpl<>(events, pageable, events.size());
		long totalElements = page.getTotalElements();//eventsInProg.getTotalElements();
		int totalPages = page.getTotalPages();//eventsInProg.getTotalPages();
		int number = page.getNumber();//eventsInProg.getNumber();
		boolean first = page.isFirst(); //eventsInProg.isFirst();
		boolean last = page.isLast();//eventsInProg.isLast();
		int numberOfElements = page.getNumberOfElements();//eventsInProg.getNumberOfElements();
		Sort sort = page.getSort();//eventsInProg.getSort();
		EventsInProgressResponseDto res = new EventsInProgressResponseDto(events, totalElements, totalPages, size,
				number, numberOfElements, first, last, sort);
		return res;
	}

}
