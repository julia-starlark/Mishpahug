package mishpahug.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Period;
import java.util.Set;

import org.springframework.stereotype.Component;

import mishpahug.domain.Address;
import mishpahug.domain.Event;
import mishpahug.domain.EventArchive;
import mishpahug.domain.User;
import mishpahug.dto.AddressDto;
import mishpahug.dto.EventForCalendarDto;
import mishpahug.dto.EventResponseDto;
import mishpahug.dto.LocationDto;
import mishpahug.dto.OwnerDto;
import mishpahug.dto.ParticipantDto;

@Component
public class DtoFactory {

	public EventArchive convertToEventArchive(Event event) {
		return EventArchive.builder().eventId(event.getEventId()).title(event.getTitle()).holiday(event.getHoliday())
				.confession(event.getConfession()).dateTimeStart(event.getDateTimeStart())
				.dateTimeFinish(event.getDateTimeFinish()).duration(event.getDuration()).address(event.getAddress())
				.food(event.getFood()).description(event.getDescription()).status(event.getStatus())
				.participants(event.getParticipants()).subscribers(event.getSubscribers()).voted(event.getVoted())
				.owner(event.getOwner()).build();
	}

	public EventResponseDto convertToEventResponseDto(Event event, Set<ParticipantDto> participants) {
		LocalDateTime eventstart = event.getDateTimeStart();
		return EventResponseDto.builder().eventId(event.getEventId()).title(event.getTitle())
				.holiday(event.getHoliday()).confession(event.getConfession())
				.date(LocalDate.of(eventstart.getYear(), eventstart.getMonth(), eventstart.getDayOfMonth()))
				.time(LocalTime.of(eventstart.getHour(), eventstart.getMinute())).duration(event.getDuration())
				.food(event.getFood()).description(event.getDescription()).status(event.getStatus())
				.participants(participants).build();
	}

	public ParticipantDto convertToParticipantDto(User u, Event event) {
		return ParticipantDto.builder().userId(u.getUserId()).fullName(u.getFirstName() + " " + u.getLastName())
				.confession(u.getConfession()).gender(u.getGender())
				.age(Period.between(u.getDateOfBirth(), LocalDate.now()).getYears()).pictureLink(u.getPictureLink())
				.maritalStatus(u.getMaritalStatus()).foodPreferences(u.getFoodPreferences()).languages(u.getLanguages())
				.rate(u.getRate()).phoneNumber(u.getPhoneNumber()).numberOfVoters(u.getNumberOfVoters())
				/* .isInvited(u.getInvitations().contains(event.getEventId()) ? false : true) */.build();
	}

	public OwnerDto convertToOwnerDto(User eventOwner) {
		return OwnerDto.builder().fullName(eventOwner.getFirstName() + " " + eventOwner.getLastName())
				.confession(eventOwner.getConfession()).gender(eventOwner.getGender())
				.age(Period.between(eventOwner.getDateOfBirth(), LocalDate.now()).getYears())
				.pictureLink(eventOwner.getPictureLink()).phoneNumber(eventOwner.getPhoneNumber())
				.maritalStatus(eventOwner.getMaritalStatus()).foodPreferences(eventOwner.getFoodPreferences())
				.languages(eventOwner.getLanguages()).rate(eventOwner.getRate())
				.numberOfVoters(eventOwner.getNumberOfVoters()).build();
	}

	public EventResponseDto convertToEventResponseDto(Event event, OwnerDto owner) {
		Address eventAddress = event.getAddress();
		double[] eventLocation = eventAddress.getLocation();
		LocationDto location = LocationDto.builder().lat(eventLocation[0]).lng(eventLocation[1]).build();
		AddressDto address = AddressDto.builder().city(eventAddress.getCity()).place_id(eventAddress.getPlace_id())
				.location(location).build();
		LocalDateTime eventstart = event.getDateTimeStart();
		return EventResponseDto.builder().eventId(event.getEventId()).title(event.getTitle())
				.holiday(event.getHoliday()).confession(event.getConfession())
				.date(LocalDate.of(eventstart.getYear(), eventstart.getMonth(), eventstart.getDayOfMonth()))
				.time(LocalTime.of(eventstart.getHour(), eventstart.getMinute())).duration(event.getDuration())
				.duration(event.getDuration()).address(address).food(event.getFood())
				.description(event.getDescription()).status(event.getStatus()).owner(owner).build();
	}

	public EventForCalendarDto convertToEventForCalendarDto(Event e) {
		LocalDateTime date = e.getDateTimeStart();
		return EventForCalendarDto.builder().eventId(e.getEventId()).title(e.getTitle())
				.date(LocalDate.of(date.getYear(), date.getMonth(), date.getDayOfMonth()))
				.time(LocalTime.of(date.getHour(), date.getMinute())).duration(e.getDuration()).status(e.getStatus())
				.owner(e.getOwner()).build();
	}
}
