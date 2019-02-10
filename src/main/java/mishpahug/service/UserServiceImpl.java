package mishpahug.service;

import java.security.Principal;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import mishpahug.configuration.UserAccountConfiguration;
import mishpahug.configuration.UserCredentials;
import mishpahug.dao.StaticFieldsRepository;
import mishpahug.dao.UserAccountRepository;
import mishpahug.domain.StaticFields;
import mishpahug.domain.User;
import mishpahug.dto.SuccessResponseDto;
import mishpahug.dto.UserResponseDto;
import mishpahug.dto.UserUpdateDto;
import mishpahug.exceptions.ConflictException;
import mishpahug.exceptions.InvalidDataException;

@Service
public class UserServiceImpl implements UserService {

	@Autowired
	UserAccountRepository userRepository;

	@Autowired
	StaticFieldsRepository staticFieldsRepository;

	@Autowired
	UserAccountConfiguration accountConfiguration;

	@Autowired
	PasswordEncoder encoder;

	
	@Transactional
	@Override
	public StaticFields addUser(String token) {
		UserCredentials creds = accountConfiguration.decodeToken(token);
		String login;
		try {
			login = creds.getLogin();
		} catch (Exception e) {
			throw new InvalidDataException("Invalid data!");
		}
		if (userRepository.existsById(login)) {
			throw new ConflictException("User exists!");
		}
		String hashPassword = encoder.encode(creds.getPassword());
		User user = new User(login, hashPassword);
		userRepository.save(user);
		StaticFields staticFields = staticFieldsRepository.findById("1").get();
		return staticFields;
	}

	@Override
	public UserResponseDto getUser(Principal principal) {
		User user = userRepository.findById(principal.getName()).orElse(null);
		if (user == null) {
			return null;
		}
		if (user.getFirstName() == null && user.getLastName() == null) {
			throw new ConflictException("User has empty profile!");
		}
		return convertToUserResponseDto(user);
	}

	private UserResponseDto convertToUserResponseDto(User user) {
		return UserResponseDto.builder().firstName(user.getFirstName()).lastName(user.getLastName())
				.dateOfBirth(user.getDateOfBirth()).gender(user.getGender()).maritalStatus(user.getMaritalStatus())
				.confession(user.getConfession()).pictureLink(user.getPictureLink()).phoneNumber(user.getPhoneNumber())
				.foodPreferences(user.getFoodPreferences()).languages(user.getLanguages())
				.description(user.getDescription()).rate(user.getRate()).numberOfVoters(user.getNumberOfVoters())
				.build();
	}

	@Override
	public UserResponseDto login(Principal principal) {
		return getUser(principal);
	}

	@Override
	@Transactional
	public UserResponseDto updateUser(UserUpdateDto userUpdateDto, Principal principal) {
		User user = userRepository.findById(principal.getName()).get();
		user.setFirstName(userUpdateDto.getFirstName());
		user.setLastName(userUpdateDto.getLastName());
		user.setDateOfBirth(userUpdateDto.getDateOfBirth());
		user.setGender(userUpdateDto.getGender());
		user.setMaritalStatus(userUpdateDto.getMaritalStatus());
		user.setConfession(userUpdateDto.getConfession());
		user.setPhoneNumber(userUpdateDto.getPhoneNumber());
		List<String> foodPreferences = userUpdateDto.getFoodPreferences();
		if (foodPreferences != null) {
			foodPreferences.stream().forEach(s -> user.addFoodPreferences(s));
		}
		List<String> languages = userUpdateDto.getLanguages();
		if (languages != null) {
			languages.stream().forEach(s -> user.addLanguages(s));
		}
		user.setDescription(userUpdateDto.getDescription());
		user.addPictureLink(userUpdateDto.getPictureLink());
		userRepository.save(user);
		return convertToUserResponseDto(user);
	}

	@Override
	@Transactional
	public UserResponseDto deleteUser(Principal principal) {
		String login = principal.getName();
		User user = userRepository.findById(login).get();
		userRepository.deleteById(login);
		return convertToUserResponseDto(user);
	}

	@Override
	@Transactional
	public SuccessResponseDto addFirebaseToken(String token, Principal principal) {
		User user = userRepository.findById(principal.getName()).get();
		if (token != null) {
			user.setFirebaseToken(token);
		}
		userRepository.save(user);
		return new SuccessResponseDto("Token is added!");
	}

	@Override
	@Transactional
	public SuccessResponseDto deleteFirebaseToken(String token, Principal principal) {
		User user = userRepository.findById(principal.getName()).get();
		if (user.getFirebaseToken().equals(token)) {
			user.setFirebaseToken(null);
			userRepository.save(user);
			return new SuccessResponseDto("Token is deleted!");
		} else {
			throw new ConflictException("User is not associated with the token!");
		}

	}

}
