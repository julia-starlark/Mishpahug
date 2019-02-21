package mishpahug.configuration;

import java.util.Base64;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class UserAccountConfiguration {
	
	@Bean
	public PasswordEncoder getPasswordEncoder() {
		return new BCryptPasswordEncoder();
	}

	public UserCredentials decodeToken(String token) {
		int index = token.indexOf(" ");
		token = token.substring(index + 1);
		byte[] base64DecodeBytes = Base64.getDecoder().decode(token);
		token = new String(base64DecodeBytes);
		String[] creds = token.split(":");
		UserCredentials credentials = new UserCredentials(creds[0].trim().toLowerCase(), creds[1]);
		return credentials;
	}
}
