package mishpahug.controller;

import java.security.Principal;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import mishpahug.dto.CalendarResponseDto;
import mishpahug.dto.EventCreateDto;
import mishpahug.dto.EventResponseDto;
import mishpahug.dto.EventsHistoryListResponseDto;
import mishpahug.dto.EventsInProgressResponseDto;
import mishpahug.dto.EventsListResponseDto;
import mishpahug.dto.InviteResponseDto;
import mishpahug.dto.SuccessResponseDto;
import mishpahug.service.EventService;

@RestController
@RequestMapping("/event")
public class EventsController {

	@Autowired
	EventService eventService;
	
	@PostMapping("/creation")
	public SuccessResponseDto addEvent(@RequestBody EventCreateDto eventCreateDto, Principal principal) {
		return eventService.addEvent(eventCreateDto, principal);
	}
	
	@GetMapping("/own/{eventId}")
	public EventResponseDto getOwnEventInfoById(@PathVariable long eventId) {
		return eventService.getOwnEventInfoById(eventId);
	}
	
	@GetMapping("/subscribed/{eventId}")
	public EventResponseDto getSubscribedEventById(@PathVariable Long eventId) {
		return eventService.getSubscribedEventById(eventId);
	}
	
	@GetMapping("/calendar/{month}")
	public CalendarResponseDto getEventsByMonth(@PathVariable int month, Principal principal) {
		return eventService.getEventsByMonth(month, principal);
	}
	
	@GetMapping("/currentlist")
	public EventsListResponseDto getOwnEventsList(Principal principal) {
		return eventService.getOwnEventsList(principal);
	}
	
	@GetMapping("/historylist")
	public EventsHistoryListResponseDto getOwnDoneEventsList(Principal principal) {
		return eventService.getOwnDoneEventsList(principal);
	}
	
	@GetMapping("/participationlist")
	public EventsListResponseDto getSubscribedEvents(Principal principal) {
		return eventService.getSubscribedEvents(principal);
	}
	
	@PutMapping("/vote/{eventId}/{voteCount}")
	public SuccessResponseDto voteForEvent(@PathVariable Long eventId, @PathVariable Double voteCount, Principal principal) {
		return eventService.voteForEvent(eventId, voteCount, principal);
	}
	
	@PutMapping("/subscription/{eventId}")
	public SuccessResponseDto subscribeToEvent(@PathVariable Long eventId, Principal principal) {
		return eventService.subscribeToEvent(eventId, principal);
	}
	
	@PutMapping("/unsubscription/{eventId}")
	public SuccessResponseDto unsubscribeFromEvent(@PathVariable Long eventId, Principal principal) {
		return eventService.unsubscribeFromEvent(eventId, principal);
	}
	
	@PutMapping("/invitation/{eventId}/{userId}")
	public InviteResponseDto inviteToEvent(@PathVariable Long eventId, @PathVariable Long userId) {
		return eventService.inviteToEvent(eventId, userId);
	}
	
	@PostMapping("/allprogresslist")
	public EventsInProgressResponseDto getAllEventsInProgress(@RequestParam int page, @RequestParam int size) {
		return eventService.getAllEventsInProgress(page, size);
	}
}
