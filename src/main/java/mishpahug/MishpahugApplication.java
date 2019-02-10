package mishpahug;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import mishpahug.dao.StaticFieldsRepository;
import mishpahug.domain.StaticFields;

@SpringBootApplication
public class MishpahugApplication implements CommandLineRunner{
	@Autowired
	StaticFieldsRepository staticFieldsRepository;

	public static void main(String[] args) {
		SpringApplication.run(MishpahugApplication.class, args);
	}

	//FIXME
	@Override
	public void run(String... args) throws Exception {
		StaticFields staticFields = new StaticFields();
		staticFieldsRepository.save(staticFields);
	}

}

