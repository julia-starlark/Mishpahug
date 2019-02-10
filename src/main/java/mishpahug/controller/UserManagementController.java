package mishpahug.controller;

import java.security.Principal;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import mishpahug.dao.StaticFieldsRepository;
import mishpahug.domain.StaticFields;
import mishpahug.dto.SuccessResponseDto;
import mishpahug.dto.UserResponseDto;
import mishpahug.dto.UserUpdateDto;
import mishpahug.service.UserService;

@RestController
@RequestMapping("/user")
public class UserManagementController {

	@Autowired
	UserService userService;

	@Autowired
	StaticFieldsRepository staticFields;

	@PostMapping("/registration")
	public StaticFields addUser(@RequestHeader("Authorization") String token) {
		return userService.addUser(token);
	}

	@GetMapping("/profile")
	public UserResponseDto getUser(Principal principal) {
		return userService.getUser(principal);
	}

	@PostMapping("/login")
	public UserResponseDto login(Principal principal) {
		return userService.getUser(principal);
	}

	@PostMapping("/profile")
	public UserResponseDto updateUser(@RequestBody UserUpdateDto userUpdateDto, Principal principal) {
		return userService.updateUser(userUpdateDto, principal);
	}

	@DeleteMapping("/profile")
	public UserResponseDto deleteUser(Principal principal) {
		return userService.deleteUser(principal);
	}

	@GetMapping("/staticfields")
	public StaticFields showStaticFields() {
		return staticFields.findAll().get(0);
	}

	@PostMapping("/firebasetoken/add")
	public SuccessResponseDto addFirebaseToken(@RequestBody String firebaseToken, Principal principal) {
		return userService.addFirebaseToken(firebaseToken, principal);

	}

	@PostMapping("/firebasetoken/delete")
	public SuccessResponseDto deleteFirebaseToken(@RequestBody String firebaseToken, Principal principal) {
		return userService.deleteFirebaseToken(firebaseToken, principal);

	}
}
