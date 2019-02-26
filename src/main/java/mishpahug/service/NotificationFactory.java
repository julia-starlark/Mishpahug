package mishpahug.service;

import java.time.format.DateTimeFormatter;

import org.springframework.stereotype.Component;

import mishpahug.domain.Notification;
import mishpahug.dto.NotificationNewDto;
import mishpahug.dto.NotificationTitle;

@Component
public class NotificationFactory {
	public Notification creteNewNotification(NotificationNewDto notificationNewDto) {
		String message = "";
		String title = "";
		if (NotificationTitle.VOTE_FOR_EVENT.equals(notificationNewDto.getTitle())) {
			title = "Vote for Event";
			message = "Don't forget to vote for event " + notificationNewDto.getEventTitle()
					+ " that you have attended.";
		}
		if (NotificationTitle.SUBSCRIPTION_TO_EVENT.equals(notificationNewDto.getTitle())) {
			title = "Subscription to Event";
			message = notificationNewDto.getUserFirstName() + " " + notificationNewDto.getUserLastName()
					+ " subscribed to your event " + notificationNewDto.getEventTitle()
					+ " that is suppossed to take place on "
					+ notificationNewDto.getDate().format(DateTimeFormatter.ISO_LOCAL_TIME) + ".";
		}
		if (NotificationTitle.UNSUBSCRIPTION_FROM_EVENT.equals(notificationNewDto.getTitle())) {
			title = "Unsubscription from Event";
			message = notificationNewDto.getUserFirstName() + " " + notificationNewDto.getUserLastName()
					+ " unsubscribed from your event " + notificationNewDto.getEventTitle()
					+ " that is suppossed to take place on "
					+ notificationNewDto.getDate().format(DateTimeFormatter.ISO_LOCAL_TIME) + ".";
		}
		if (NotificationTitle.NEW_VOTE.equals(notificationNewDto.getTitle())) {
			title = "New Vote";
			message = notificationNewDto.getUserFirstName() + " " + notificationNewDto.getUserLastName()
					+ " has voted for the event " + notificationNewDto.getEventTitle();
		}
		if(NotificationTitle.EVENT_CANCELATION.equals(notificationNewDto.getTitle())) {
			title = "Event Cancelation";
			message = "We are sorry to inform you that the event " + notificationNewDto.getEventTitle()
					+ " that was suppossed to take place on " + notificationNewDto.getDate().format(DateTimeFormatter.ISO_LOCAL_TIME)
					+ " was cancelled.";
		}
		return new Notification(title, message, notificationNewDto.getEventId());
	}
}
