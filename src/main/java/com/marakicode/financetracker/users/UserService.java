package com.marakicode.financetracker.users;

import com.marakicode.financetracker.common.DuplicateResourceException;
import com.marakicode.financetracker.common.EmailNormalizer;
import com.marakicode.financetracker.common.PagedResponse;
import com.marakicode.financetracker.common.ResourceNotFoundException;
import com.marakicode.financetracker.users.dto.PasswordUpdateRequest;
import com.marakicode.financetracker.users.dto.UserCreateRequest;
import com.marakicode.financetracker.users.dto.UserDto;
import com.marakicode.financetracker.users.dto.UserUpdateRequest;
import com.marakicode.financetracker.users.exceptions.LastAdminActionException;
import com.marakicode.financetracker.users.exceptions.PasswordMismatchException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;

    @Transactional
    public UserDto createUser(UserCreateRequest request) {
        String normalizedEmail = EmailNormalizer.normalize(request.email());
        validateUniqueEmail(normalizedEmail);
        User user = userMapper.toEntity(request);
        user.setEmail(normalizedEmail);
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        try {
            User saved = userRepository.save(user);
            log.info("event=user.created userId={} email={}", saved.getId(), normalizedEmail);
            return userMapper.toDto(saved);
        } catch (DataIntegrityViolationException e) {
            throw new DuplicateResourceException("Email already registered");
        }
    }

    @Transactional(readOnly = true)
    public User findByEmail(String email) {
        return userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));
    }

    @Transactional(readOnly = true)
    public User findById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
    }

    @Transactional(readOnly = true)
    public UserDto getUserByEmail(String email) {
        User user = findByEmail(email);
        return userMapper.toDto(user);
    }

    @Transactional(readOnly = true)
    public UserDto getUserById(Long id) {
        User user = findById(id);
        return userMapper.toDto(user);
    }

    @Transactional(readOnly = true)
    public PagedResponse<UserDto> getAllUsers(Pageable pageable) {
        var page = userRepository.findAll(pageable);
        return PagedResponse.fromPage(page, userMapper::toDto);
    }

    @Transactional
    public UserDto updateUser(Long id, UserUpdateRequest request) {
        User user = findById(id);
        userMapper.updateEntity(request, user);
        User saved = userRepository.save(user);
        log.info("event=user.updated userId={}", id);
        return userMapper.toDto(saved);
    }

    @Transactional
    public void updatePassword(Long id, PasswordUpdateRequest request) {
        User user = findById(id);
        if (!passwordEncoder.matches(request.oldPassword(), user.getPasswordHash())) {
            log.warn("event=user.password_update_failed userId={} reason=old_password_mismatch", id);
            throw new PasswordMismatchException("Current password is incorrect");
        }
        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);
        log.info("event=user.password_updated userId={}", id);
    }

    @Transactional
    public void deleteUser(Long id) {
        User user = findById(id);
        if (user.getRole() == Role.ADMIN && userRepository.countByRoleAndActiveTrue(Role.ADMIN) <= 1) {
            throw new LastAdminActionException("Cannot delete the last admin user");
        }
        userRepository.delete(user);
        log.info("event=user.deleted userId={}", id);
    }

    private void validateUniqueEmail(String email) {
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new DuplicateResourceException("Email already registered");
        }
    }
}
