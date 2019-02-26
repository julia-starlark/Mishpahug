package mishpahug.service;

import java.security.Principal;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import mishpahug.dao.UserAccountRepository;
import mishpahug.domain.Notification;
import mishpahug.domain.User;
import mishpahug.dto.NotificationDto;
import mishpahug.dto.NotificationNewDto;
import mishpahug.dto.NotificationsCountDto;
import mishpahug.dto.NotificationsListDto;
import mishpahug.dto.SuccessResponseDto;

@Service
public class NotificationServiceImpl implements NotificationService {

	@Autowired
	UserAccountRepository userRepository;
	
	@Override
	public NotificationsListDto getNotificationList(Principal principal) {
		List<Notification> nots = userRepository.findById(principal.getName()).get().getNotifications();
		nots.removeIf(n -> n.getDate().isBefore(LocalDate.now().minusMonths(1)));
		NotificationsListDto nld = new NotificationsListDto(
				nots.stream().map(n -> new NotificationDto(n.getNotificationId(), n.getTitle(), n.getMessage(),
						n.getDate(), n.getType(), n.isRead(), n.getEventId())).collect(Collectors.toList()));
		return nld;
	}

	@Override
	public NotificationsCountDto getNumberOfUnreadNotifications(Principal principal) {
		int notificationsCount = userRepository.findById(principal.getName()).get().getNotifications().size();
		NotificationsCountDto count = new NotificationsCountDto(notificationsCount);
		return count;
	}

	@Override
	@Transactional
	public SuccessResponseDto readNotification(long notificationId, Principal principal) {
		User user = userRepository.findById(principal.getName()).get();
		Notification note = user.getNotifications().stream().filter(n -> n.getNotificationId() == notificationId)
				.findFirst().orElse(null);
		note.setRead(true);
		userRepository.save(user);
		return new SuccessResponseDto("Notification is updated!");
	}

	@Override
	public boolean deleteNotification(long notificationId, Principal principal) {
		// FIXME check if the user is the owner
		User user = userRepository.findById(principal.getName()).get();
		return user.getNotifications().removeIf(n -> n.getNotificationId() == notificationId);
	}

}
