package mishpahug.service;

import mishpahug.dto.NotificationDto;
import mishpahug.dto.NotificationNewDto;
import mishpahug.dto.NotificationsCountDto;
import mishpahug.dto.NotificationsListDto;

public interface NotificationService {

	boolean sendNotification(Long userId, NotificationNewDto notificationNewDto);

	NotificationDto getNotification(Long notificationId);

	NotificationsListDto getNotificationList();

	NotificationsCountDto getNumberOfUnreadNotifications();

	boolean updateNotifiation(Long notificationId);

	boolean deleteNotification(Long notificationId);
}
