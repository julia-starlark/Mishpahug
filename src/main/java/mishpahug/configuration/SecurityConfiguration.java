package mishpahug.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.google.common.collect.ImmutableList;

@Configuration
@EnableWebSecurity
public class SecurityConfiguration extends WebSecurityConfigurerAdapter {

	@Override
	public void configure(WebSecurity web) {
		web.ignoring().antMatchers(HttpMethod.POST, "/user/registration");
	}

	@Override
	protected void configure(HttpSecurity http) throws Exception {
		http.httpBasic();
		http.csrf().disable();
		http.cors();
		// http.sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS);
		http.authorizeRequests().antMatchers("/user/staticfields", "/event/allprogresslist/**").permitAll();
		http.authorizeRequests().antMatchers(HttpMethod.GET, "/event/own/{eventId}")
				.access("@securityCheck.checkEventOwnership(authentication,#eventId)");
		http.authorizeRequests().antMatchers(HttpMethod.GET, "/event/subscribed/{eventId}")
				.access("@securityCheck.checkEventSubscription(authentication,#eventId)");
		http.authorizeRequests().antMatchers(HttpMethod.PUT, "/event/unsubscription/{eventId}")
				.access("@securityCheck.checkEventSubscription(authentication, #eventId)");
		http.authorizeRequests()
				.antMatchers(HttpMethod.PUT, "/event/invitation/{eventId}/*", "/event/pending/{eventId}")
				.access("@securityCheck.checkEventOwnership(authentication, #eventId)");
		http.authorizeRequests().antMatchers(HttpMethod.PUT, "/notification/isRead/{notificationId}")
				.access("@securityCheck.checkNotificationOwnership(authentication, #notificationId)");
		http.authorizeRequests().antMatchers(HttpMethod.DELETE, "/notification/{notificationId}")
				.access("@securityCheck.checkNotificationOwnership(authentication, #notificationId)");
		http.authorizeRequests().antMatchers(HttpMethod.POST, "/user/login", "/event/creation", "/user/profile",
				"/user/firebasetoken/add").authenticated();
		http.authorizeRequests()
				.antMatchers(HttpMethod.GET, "/user/profile", "/event/calendar/*", "/notification/list",
						"/event/currentlist", "/event/historylist", "/event/participationlist", "/notification/count")
				.authenticated();
		http.authorizeRequests().antMatchers(HttpMethod.PUT, "/event/subscription/*").authenticated();
		http.authorizeRequests().antMatchers(HttpMethod.DELETE, "/user/firebasetoken/delete").authenticated();
	}

	@Bean
	public CorsConfigurationSource corsConfigurationSource() {
		final CorsConfiguration configuration = new CorsConfiguration();
		configuration.setAllowedOrigins(ImmutableList.of("*"));
		configuration.setAllowedMethods(ImmutableList.of("HEAD", "GET", "POST", "PUT", "DELETE", "PATCH"));
		// setAllowCredentials(true) is important, otherwise:
		// The value of the 'Access-Control-Allow-Origin' header in the response must
		// not be the wildcard '*' when the request's credentials mode is 'include'.
		configuration.setAllowCredentials(true);
		// setAllowedHeaders is important! Without it, OPTIONS preflight request
		// will fail with 403 Invalid CORS request
		configuration.setAllowedHeaders(ImmutableList.of("Authorization", "Cache-Control", "Content-Type"));
		final UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
		source.registerCorsConfiguration("/**", configuration);
		return source;
	}
}
