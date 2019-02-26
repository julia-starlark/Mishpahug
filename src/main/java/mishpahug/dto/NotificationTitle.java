package mishpahug.dto;

import com.fasterxml.jackson.annotation.JsonFormat;

@JsonFormat(shape=JsonFormat.Shape.STRING)
public enum NotificationTitle {
	VOTE_FOR_EVENT, SUBSCRIPTION_TO_EVENT, UNSUBSCRIPTION_FROM_EVENT, NEW_VOTE, EVENT_CANCELATION, INVITATION
}
