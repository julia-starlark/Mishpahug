package mishpahug.dao;

import org.springframework.data.mongodb.repository.MongoRepository;

import mishpahug.domain.StaticFields;

public interface StaticFieldsRepository extends MongoRepository<StaticFields, String> {

}
