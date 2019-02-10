package mishpahug.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AddressDto {
	String city;
	@Setter
	@JsonInclude(Include.NON_NULL)
	String place_id;
	@Setter
	@JsonInclude(Include.NON_NULL)
	LocationDto location;
}
