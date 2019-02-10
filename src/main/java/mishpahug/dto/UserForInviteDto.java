package mishpahug.dto;

import java.util.Set;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor

public class UserForInviteDto {

	Long userId;
	String login;
	Set<Long> invitations;
}
