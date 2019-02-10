package mishpahug.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StaticFieldsDto {
	List<String> confession;
	List<String> gender;
	List<String> maritalStatus;
	List<String> foodPreferences;
	List<String> languages;
	List<String> holiday;
}
