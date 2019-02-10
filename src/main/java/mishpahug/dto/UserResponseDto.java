package mishpahug.dto;

import java.time.LocalDate;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonFormat;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Singular;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserResponseDto {
	String firstName;
	String lastName;
	@JsonFormat(pattern="yyyy-MM-dd")
	LocalDate dateOfBirth;
	String gender;
	String maritalStatus;
	String confession;
	String[] pictureLink;
	String phoneNumber;
	@Singular
	List<String> foodPreferences;
	@Singular
	List<String> languages;
	String description;
	Double rate;
	int numberOfVoters;
}
