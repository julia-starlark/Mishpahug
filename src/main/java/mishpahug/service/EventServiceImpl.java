package mishpahug.service;

import java.security.Principal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import mishpahug.dao.ArchiveRepository;
import mishpahug.dao.EventsRepository;
import mishpahug.dao.UserAccountRepository;
import mishpahug.domain.Address;
import mishpahug.domain.Event;
import mishpahug.domain.EventArchive;
import mishpahug.domain.Location;
import mishpahug.domain.User;
import mishpahug.dto.AddressDto;
import mishpahug.dto.CalendarResponseDto;
import mishpahug.dto.EventCreateDto;
import mishpahug.dto.EventDateTimeDto;
import mishpahug.dto.EventForCalendarDto;
import mishpahug.dto.EventResponseDto;
import mishpahug.dto.EventStatusResponseDto;
import mishpahug.dto.EventsHistoryListResponseDto;
import mishpahug.dto.EventsListResponseDto;
import mishpahug.dto.InviteResponseDto;
import mishpahug.dto.LocationDto;
import mishpahug.dto.OwnerDto;
import mishpahug.dto.ParticipantDto;
import mishpahug.dto.SuccessResponseDto;
import mishpahug.dto.UserForInviteDto;
import mishpahug.exceptions.ConflictException;

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
	// FIXME check that status have been changed to pending, change status to done
	// after finishing
	public SuccessResponseDto addEvent(EventCreateDto eventCreateDto, Principal principal) {
		Location location = new Location(eventCreateDto.getAddress().getLocation().getLat(),
				eventCreateDto.getAddress().getLocation().getLng(),
				eventCreateDto.getAddress().getLocation().getRadius());
		Address address = new Address(eventCreateDto.getAddress().getCity(), eventCreateDto.getAddress().getPlace_id(),
				location);
		Event event = new Event(eventCreateDto.getTitle(), eventCreateDto.getHoliday(), eventCreateDto.getConfession(),
				eventCreateDto.getDate(), eventCreateDto.getTime(), eventCreateDto.getDuration(), address,
				eventCreateDto.getFood(), eventCreateDto.getDescription(), principal.getName());
		/*
		 * if (event.getDate().isBefore(LocalDate.now().plusDays(2))) { throw new
		 * InvalidDataException("Invalid data!"); }
		 */
		List<EventDateTimeDto> ownerEvents = eventsRepository.findEventDateTimeByOwner(event.getOwner());
		LocalDateTime eventStart = LocalDateTime.of(event.getDate(), event.getTime());
		// FIXME event starts after start, but before finish
		List<EventDateTimeDto> eventOverlap = ownerEvents.stream()
				.filter(e -> LocalDateTime.of(e.getDate(), e.getTime()).isBefore(eventStart)
						&& LocalDateTime.of(e.getDate(), e.getTime()).plusMinutes(e.getDuration()).isAfter(eventStart))
				.collect(Collectors.toList());
		if (!eventOverlap.isEmpty()) {
			throw new ConflictException("This user has already created the event on this date and time!");
		}
		eventsRepository.save(event);
		checkForPendingStatus(event);
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
			}

		}, delay, TimeUnit.MINUTES);
	}

	private void checkForPendingStatus(Event event) {
		// FIXME send notifications to the participants
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
		Set<User> users = event.getParticipants().stream().map(id -> userRepository.findById(id).get())
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
		//FIXME daysInMonth +1 
		LocalDate dateFrom = LocalDate.of(LocalDate.now().getYear(), month, 1);
		YearMonth yearMonth = YearMonth.of(LocalDate.now().getYear(), month);
		int daysInMonth = yearMonth.lengthOfMonth();
		LocalDate dateTo = dateFrom.plusDays(daysInMonth);
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
		myEvents.sort((e1, e2) -> {
			int res = e1.getDate().compareTo(e2.getDate());
			if (res != 0) {
				return res;
			}
			return e1.getTime().compareTo(e2.getTime());
		});
		List<EventResponseDto> eventsRespDto = new ArrayList<>();
		for (Event e : myEvents) {
			Set<ParticipantDto> participants = e.getParticipants().stream().map(id -> userRepository.findById(id).get())
					.map(u -> convertToParticipantDto(u, e)).collect(Collectors.toSet());
			if (e.getStatus().equals("in progress")) {
				participants.forEach(p -> p.setPhoneNumber(null));
			}
			eventsRespDto.add(convertToEventResponseDto(e, participants));
		}
		myEvents.forEach(e -> System.out.println(e));
		EventsListResponseDto events = new EventsListResponseDto(eventsRespDto);
		return events;
	}

	@Override
	public EventsHistoryListResponseDto getOwnDoneEventsList(Principal principal) {
		List<EventResponseDto> ownEvents = archiveRepository.findByOwner(principal.getName());
		EventsHistoryListResponseDto events = new EventsHistoryListResponseDto(ownEvents);
		return events;
	}

	@Override
	public EventsListResponseDto getSubscribedEvents(Principal principal) {
		//FIXME
		//FIXME User invitations clear events that finished
		String userLogin = principal.getName();
		List<EventResponseDto> eventsArchive = archiveRepository.findEventsByParticipantsContains(userLogin);
		
		
		return null;
	}

	@Override
	public SuccessResponseDto subscribeToEvent(Long eventId, Principal principal) {
		//FIXME check for other subscribtions on the same date
		Event event = eventsRepository.findById(eventId).orElse(null);
		String userLogin = principal.getName();
		if(event.getOwner().equals(userLogin) || event.getParticipants().contains(userLogin)) {
			throw new ConflictException("User is the owner of the event or already subscribed to it!");
		}
		event.addParticipant(principal.getName());
		eventsRepository.save(event);
		return new SuccessResponseDto("User subscribed to the event!");
	}

	@Override
	public SuccessResponseDto unsubscribeFromEvent(Long eventId, Principal principal) {
		//FIXME check if user is subscribed for the event
		Event event = eventsRepository.findById(eventId).orElse(null);
		if(!event.getStatus().equals("in progress")) {
			throw new ConflictException("User can't unsubscribe from the event!");
		}
		event.deleteParticipant(principal.getName());
		eventsRepository.save(event);
		return new SuccessResponseDto("User unsubscribed from the event!");
	}

	@Override
	@Transactional
	public SuccessResponseDto voteForEvent(Long eventId, Double voteCount, Principal principal) {
		// FIXME web security check if user was a participant of the event
		Event event = eventsRepository.findById(eventId).orElse(null);
		// FIXME how not to get the whole user?
		List<User> participants = userRepository.findParticipants(event.getParticipants(), eventId);
		List<String> userNames = new ArrayList<>();
		String userLogin = principal.getName();
		for(User u : participants) {
			userNames.add(u.getLogin());
		}
		Set<String> voted = event.getVoted();
		if (voted.contains(userLogin) || !userNames.contains(userLogin)) {
			throw new ConflictException("User has already voted for the event or can't vote for the event!");
		}
		User owner = userRepository.findById(event.getOwner()).get();
		double currentRate = owner.getRate();
		int numVoters = owner.getNumberOfVoters();
		double newRate = (currentRate*numVoters + voteCount)/++numVoters;
		owner.setRate(newRate);
		owner.setNumberOfVoters(numVoters++);
		userRepository.save(owner);
		event.getVoted().add(userLogin);
		eventsRepository.save(event);
		return new SuccessResponseDto("User vote is accepted!");
	}

	@Override
	public InviteResponseDto inviteToEvent(Long eventId, Long userId) {
		//FIXME check that the user is the owner of the event
		//FIXME send notification to user
		Event event = eventsRepository.findById(eventId).orElse(null);
		User user = userRepository.findUserByUserId(userId);
		String login = user.getLogin();
		if(!event.getParticipants().contains(login) || user.getInvitations().contains(userId)) {
			throw new ConflictException("User is already invited to the event or is not subscribed to the event!");
		}
		user.addInvitation(eventId);
		//check if user has other subscriptions on the date and cancel them
		List<Event> eventsOverlap = eventsRepository.findDateOverlapForUser(login, event.getDate());
		if(!eventsOverlap.isEmpty()) {
			for(Event e : eventsOverlap) {
				e.deleteParticipant(login);
				eventsRepository.save(e);
			}
		}
		userRepository.save(user);
		return new InviteResponseDto(userId, true);
	}

	@Override
	public EventStatusResponseDto changeEventStatus(Long eventId) {
		//FIXME security check that the user is the owner of the event
		Event event = eventsRepository.findById(eventId).orElse(null);
		event.setStatus("pending");
		eventsRepository.save(event);
		return new EventStatusResponseDto(eventId, "Pending");
	}

}
