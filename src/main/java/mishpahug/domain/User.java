package mishpahug.domain;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import mishpahug.exceptions.InvalidDataException;

@Getter
@NoArgsConstructor
@EqualsAndHashCode(of = {"login"})
@Document
@ToString
public class User {
	long userId;
	@Id
	@Field("_id")
	String login;
	String password;
	String firstName;
	String lastName;
	LocalDate dateOfBirth;
	String gender;
	String maritalStatus;
	String confession;
	String phoneNumber;
	String[] pictureLink;
	List<String> foodPreferences;
	List<String> languages;
	String description;
	Double rate;
	int numberOfVoters;
	Set<Long> invitations;
	List<Notification> notifications;
	@Setter
	String firebaseToken;

	public User(String login, String password) {
		this.login = login;
		this.password = password;
		this.userId = System.currentTimeMillis();
		this.rate = 0.;
		this.numberOfVoters = 0;
		this.pictureLink = new String[2];
		this.foodPreferences = new ArrayList<>();
		this.languages = new ArrayList<>();
		this.invitations = new HashSet<>();
		this.notifications = new ArrayList<>();
	}
/**
 * <p>Manages profile avatar and banner. To delete user avatar the first element of the array should be passed as <b>null</b>.
 *  To delete banner the second array element should be passed as <b>null</b>. To update one of the images without changing the second one you should
 *   pass a new link to the picture you want to update and the old link to the picture that stays unchanged.</p>
 * @param links - array of two elements: links[0] - link to user avatar, links[1] - link to user banner
 * @return true - if the pictures were successfully updated
 * @throws InvalidDataException 
 */
	public boolean addPictureLink(String[] links) {
		try {
			String avatar = links[0];
			String banner = links[1];
			if (avatar != null && (pictureLink[0] == null || !pictureLink[0].equals(avatar))) {
				pictureLink[0] = avatar;
			}
			if (avatar == null) {
				pictureLink[0] = null;
			}
			if (banner != null && (pictureLink[1] == null || !pictureLink[1].equals(banner))) {
				pictureLink[1] = banner;
			}
			if (banner == null) {
				pictureLink[1] = null;
			}
		} catch (Exception e) {
			throw new InvalidDataException( "User data is invalid!");
		}
		return true;

	}

	public boolean addFoodPreferences(String food) {
		if (!foodPreferences.contains(food)) {
			return foodPreferences.add(food);
		}
		return false;
	}

	public boolean deleteFoodPreferences(String food) {
		return foodPreferences.remove(food);
	}

	public boolean addLanguages(String language) {
		if (!languages.contains(language)) {
			return languages.add(language);
		}
		return false;
	}

	public boolean deleteLanguage(String language) {
		return languages.remove(language);
	}

	public boolean addInvitation(long eventId) {
		return invitations.add(eventId);
	}

	public boolean deleteInvitation(long eventId) {
		return invitations.remove(eventId);
	}

	public boolean addNotification(Notification notification) {
		return notifications.add(notification);
	}

	public boolean deleteNotification(Notification notification) {
		return notifications.remove(notification);
	}

	public void setFirstName(String firstName) {
		if (firstName != null) {
			this.firstName = firstName;
		}
	}

	public void setLastName(String lastName) {
		if (lastName != null) {
			this.lastName = lastName;
		}
	}

	public void setDateOfBirth(LocalDate dateOfBirth) {
		if (dateOfBirth != null) {
			this.dateOfBirth = dateOfBirth;
		}
	}

	public void setGender(String gender) {
		if (gender != null) {
			this.gender = gender;
		}
	}

	public void setMaritalStatus(String maritalStatus) {
		if (maritalStatus != null) {
			this.maritalStatus = maritalStatus;
		}
	}

	public void setConfession(String confession) {
		if (confession != null) {
			this.confession = confession;
		}
	}

	public void setPhoneNumber(String phoneNumber) {
		if (phoneNumber != null) {
			this.phoneNumber = phoneNumber;
		}
	}

	public void setDescription(String description) {
		if (description != null) {
			this.description = description;
		}
	}

	public void setRate(Double rate) {
		this.rate = rate;
	}

	public void setNumberOfVoters(int numberOfVoters) {
		this.numberOfVoters = numberOfVoters;
	}

}
