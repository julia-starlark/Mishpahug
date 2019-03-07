package mishpahug.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.Singular;

@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ParticipantDto {
	Long userId;
	String fullName;
	String confession;
	String gender;
	int age;
	String[] pictureLink;
	@JsonInclude(Include.NON_NULL)
	@Setter
	String phoneNumber;
	String maritalStatus;
	@Singular
	List<String> foodPreferences;
	@Singular
	List<String> languages;
	Double rate;
	int numberOfVoters;
	boolean isInvited;
	
	public Long getUserId() {
		return userId;
	}
	public String getFullName() {
		return fullName;
	}
	public String getConfession() {
		return confession;
	}
	public String getGender() {
		return gender;
	}
	public int getAge() {
		return age;
	}
	public String[] getPictureLink() {
		return pictureLink;
	}
	public String getPhoneNumber() {
		return phoneNumber;
	}
	public String getMaritalStatus() {
		return maritalStatus;
	}
	public List<String> getFoodPreferences() {
		return foodPreferences;
	}
	public List<String> getLanguages() {
		return languages;
	}
	public Double getRate() {
		return rate;
	}
	public int getNumberOfVoters() {
		return numberOfVoters;
	}
	public boolean isIsInvited() {
		return isInvited;
	}
}
