package mishpahug.controller;

import java.security.Principal;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import mishpahug.dto.NotificationsCountDto;
import mishpahug.dto.NotificationsListDto;
import mishpahug.dto.SuccessResponseDto;
import mishpahug.service.NotificationService;

@RestController
@RequestMapping("/notification")
public class NotificationController {
	
	@Autowired
	NotificationService notificationService;
	
	@GetMapping("/list")
	public NotificationsListDto getNotificationList(Principal principal) {
		return notificationService.getNotificationList(principal);
	}

	@GetMapping("/count")
	public NotificationsCountDto getNumberOfUnreadNotifications(Principal principal) {
		return notificationService.getNumberOfUnreadNotifications(principal);
	}
	
	@PutMapping("/isRead/{notificationId}")
	public SuccessResponseDto readNotification(@PathVariable long notificationId, Principal principal) {
		return notificationService.readNotification(notificationId, principal);
	}
	
	@DeleteMapping("/{notificationId}")
	public boolean deleteNotification(@PathVariable long notificationId, Principal principal) {
		return notificationService.deleteNotification(notificationId, principal);
	}
}
