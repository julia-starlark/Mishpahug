package mishpahug.service;

import java.security.Principal;

import mishpahug.dto.CalendarResponseDto;
import mishpahug.dto.EventCreateDto;
import mishpahug.dto.EventResponseDto;
import mishpahug.dto.EventStatusResponseDto;
import mishpahug.dto.EventsHistoryListResponseDto;
import mishpahug.dto.EventsInProgressResponseDto;
import mishpahug.dto.EventsListResponseDto;
import mishpahug.dto.FiltersDto;
import mishpahug.dto.InviteResponseDto;
import mishpahug.dto.SuccessResponseDto;

public interface EventService {

	SuccessResponseDto addEvent(EventCreateDto eventCreateDto, Principal principal);

	EventResponseDto getOwnEventInfoById(Long eventId);
	
	EventResponseDto getSubscribedEventById(Long eventId);
	
	CalendarResponseDto getEventsByMonth(int month, Principal principal);

	EventsListResponseDto getOwnEventsList(Principal principal);

	EventsHistoryListResponseDto getOwnDoneEventsList(Principal principal);

	EventsListResponseDto getSubscribedEvents(Principal principal);

	SuccessResponseDto subscribeToEvent(Long eventId, Principal principal);

	SuccessResponseDto unsubscribeFromEvent(Long eventId, Principal principal);

	SuccessResponseDto voteForEvent(Long eventId, Double voteCount, Principal principal);

	InviteResponseDto inviteToEvent(Long eventId, Long userId);

	EventStatusResponseDto changeEventStatus(Long eventId);
	
	EventsInProgressResponseDto getAllEventsInProgress(int page, int size, FiltersDto filters);
}
