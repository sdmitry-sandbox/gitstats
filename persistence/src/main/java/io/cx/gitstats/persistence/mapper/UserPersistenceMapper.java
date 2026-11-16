package io.cx.gitstats.persistence.mapper;

import io.cx.gitstats.domain.model.User;
import io.cx.gitstats.persistence.entity.UserEntity;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper
public interface UserPersistenceMapper {
    User toDomain(UserEntity entity);
    UserEntity toEntity(User domain);
    List<User> toDomainList(List<UserEntity> entities);
}
