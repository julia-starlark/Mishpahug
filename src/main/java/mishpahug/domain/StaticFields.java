package mishpahug.domain;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.springframework.data.mongodb.core.mapping.Document;

import lombok.Getter;

@Getter
@Document
public class StaticFields {
	String id = "1";
	List<String> confession;
	List<String> gender;
	List<String> maritalStatus;
	List<String> foodPreferences;
	List<String> languages;
	List<String> holiday;

	public StaticFields() {
		this.confession = new ArrayList<>(Arrays.asList("religious", "irreligious"));
		this.gender = new ArrayList<>(Arrays.asList("male", "female"));
		this.maritalStatus = new ArrayList<>(Arrays.asList("married", "single", "couple"));
		this.foodPreferences = new ArrayList<>(Arrays.asList("vegetarian", "kosher", "non-vegetarian"));
		this.languages = new ArrayList<>(Arrays.asList("Hebrew", "English", "Russian"));
		this.holiday = new ArrayList<>(Arrays.asList("Pesah", "Shabbat", "Other"));

	}

	boolean addConfession(String confession) {
		if (this.confession.contains(confession)) {
			return false;
		}
		return this.confession.add(confession);
	}

	boolean deleteConfession(String confession) {
		return this.confession.remove(confession);
	}

	boolean addGender(String gender) {
		if (this.gender.contains(gender)) {
			return false;
		}
		return this.gender.add(gender);
	}

	boolean deleteGender(String gender) {
		return this.gender.remove(gender);
	}

	boolean addMaritalStatus(String status) {
		if (this.maritalStatus.contains(status)) {
			return false;
		}
		return this.maritalStatus.add(status);
	}

	boolean deleteMaritalStatus(String status) {
		return this.maritalStatus.remove(status);
	}

	boolean addFoodPreferences(String foodPreferences) {
		if (this.foodPreferences.contains(foodPreferences)) {
			return false;
		}
		return this.foodPreferences.add(foodPreferences);
	}

	boolean deleteFoodPreferences(String foodPreferences) {
		return this.foodPreferences.remove(foodPreferences);
	}

	boolean addLanguages(String language) {
		if (this.languages.contains(language)) {
			return false;
		}
		return this.languages.add(language);
	}

	boolean deleteLanguages(String language) {
		return this.languages.remove(language);
	}

	boolean addHoliday(String holiday) {
		if (this.holiday.contains(holiday)) {
			return false;
		}
		return this.holiday.add(holiday);
	}

	boolean deleteHoliday(String holiday) {
		return this.holiday.remove(holiday);
	}

}
