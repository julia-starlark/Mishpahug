package mishpahug.service.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import mishpahug.dao.UserAccountRepository;
import mishpahug.domain.User;
import mishpahug.exceptions.ConflictException;

@Service
public class UserDetailServiceImpl implements UserDetailsService {
	
	@Autowired
	UserAccountRepository userRepository;

	@Override
	public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
		User user = userRepository.findById(username).orElseThrow(() -> new ConflictException("User not found!"));
		String password = user.getPassword();
		return new org.springframework.security.core.userdetails.User(username, password, AuthorityUtils.NO_AUTHORITIES);
	}

}
