package mishpahug.domain;

import org.springframework.data.mongodb.core.index.GeoSpatialIndexed;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "place_id")
public class Address {
	String city;
	String place_id;
	@Setter
	@GeoSpatialIndexed
	double[] location;
}
