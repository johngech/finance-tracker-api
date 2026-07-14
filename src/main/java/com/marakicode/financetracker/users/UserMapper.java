package com.marakicode.financetracker.users;

import com.marakicode.financetracker.users.dto.UserCreateRequest;
import com.marakicode.financetracker.users.dto.UserDto;
import com.marakicode.financetracker.users.dto.UserUpdateRequest;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface UserMapper {

    /**
     * Maps UserCreateRequest to User entity.
     * NOTE: email is ignored here — the service layer is responsible for
     * normalizing the email to lowercase and setting it on the entity.
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "email", ignore = true)
    @Mapping(target = "passwordHash", ignore = true)
    @Mapping(target = "role", expression = "java(com.marakicode.financetracker.users.Role.USER)")
    User toEntity(UserCreateRequest request);

    UserDto toDto(User user);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "email", ignore = true)
    @Mapping(target = "passwordHash", ignore = true)
    @Mapping(target = "role", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateEntity(UserUpdateRequest request, @MappingTarget User user);
}
