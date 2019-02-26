package mishpahug.service.security;


import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import mishpahug.dao.EventsRepository;
import mishpahug.dao.UserAccountRepository;
import mishpahug.domain.Event;
import mishpahug.domain.User;
import mishpahug.exceptions.ConflictException;

@Component
public class SecurityCheck {
	
	@Autowired
	UserAccountRepository userRepository;
	
	@Autowired
	EventsRepository eventsRepository;
	
	public boolean checkEventOwnership(Authentication authentication, long eventId) {
		Event event = eventsRepository.findById(eventId).orElse(null);
		if(event == null) {
			return false;
		}
		String owner = event.getOwner();
		if(!owner.equals(authentication.getName())) {
			return false;
			//throw new ConflictException("User is not associated with the event!");
		}
		return true;
	}
	
	public boolean checkEventSubscription(Authentication authentication, Long eventId) {
		Event event = eventsRepository.findById(eventId).orElse(null);
		if(event == null) {
			return false;
		}
		Set<String> subscribers = event.getSubscribers();
		if(!subscribers.contains(authentication.getName())) {
			throw new ConflictException("User is not associated with the event!");
		}
		return true;
	}
	
	public boolean checkEventParticipation(Authentication authentication, Long eventId) {
		Event event = eventsRepository.findById(eventId).orElse(null);
		if(event == null) {
			return false;
		}
		Set<String> participants = event.getParticipants();
		Set<String> voted = event.getVoted();
		if (voted.contains(authentication.getName()) || !participants.contains(authentication.getName())) {
			throw new ConflictException("User has already voted for the event or can't vote for the event!");
		}
		return true;
	}
	
	public boolean checkNotificationOwnership(Authentication authentication, Long notificationId) {
		User user = userRepository.findById(authentication.getName()).get();
		if(user.getNotifications().stream().anyMatch(n->n.getNotificationId() == notificationId )) {
			return true;
		}
		return false;
	}

}
