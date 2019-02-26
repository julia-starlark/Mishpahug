package mishpahug.service;

import java.security.Principal;

import mishpahug.dto.NotificationsCountDto;
import mishpahug.dto.NotificationsListDto;
import mishpahug.dto.SuccessResponseDto;

public interface NotificationService {
	
	NotificationsListDto getNotificationList(Principal principal);

	NotificationsCountDto getNumberOfUnreadNotifications(Principal principal);
	
	SuccessResponseDto readNotification(long notificationId, Principal principal);
	
	boolean deleteNotification(long notificationId, Principal principal);

}
