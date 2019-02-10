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

@Getter
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
}
