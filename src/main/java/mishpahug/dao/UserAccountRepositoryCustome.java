package mishpahug.dao;

import mishpahug.domain.Notification;

public interface UserAccountRepositoryCustome {

	String getUserFullName(String login);
	
	void addNotificationToUser(String login, Notification notification);
	
}
