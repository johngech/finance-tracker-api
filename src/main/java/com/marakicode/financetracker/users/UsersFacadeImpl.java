package com.marakicode.financetracker.users;

import com.marakicode.financetracker.common.PagedResponse;
import com.marakicode.financetracker.common.ResourceNotFoundException;
import com.marakicode.financetracker.common.SearchUtils;
import com.marakicode.financetracker.users.dto.UserStatistics;
import com.marakicode.financetracker.users.dto.UserSummary;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class UsersFacadeImpl implements UsersFacade {

    private static final String UPPER = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final String LOWER = "abcdefghijklmnopqrstuvwxyz";
    private static final String DIGITS = "0123456789";
    private static final String SPECIAL = "@#$%^&+=!";
    private static final String ALL_CHARS = UPPER + LOWER + DIGITS + SPECIAL;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final int TEMP_PASSWORD_LENGTH = 12;

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional(readOnly = true)
    public UserSummary getUserById(Long userId) {
        var user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
        return toSummary(user);
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<UserSummary> listUsers(String search, Pageable pageable) {
        var spec = buildSearchSpec(search);
        var page = userRepository.findAll(spec, pageable);
        return PagedResponse.fromPage(page, this::toSummary);
    }

    @Override
    @Transactional(readOnly = true)
    public UserStatistics getStatistics() {
        Object[] row = flattenResult(userRepository.getUserStatistics());
        return new UserStatistics(
            ((Number) row[0]).longValue(),
            ((Number) row[1]).longValue(),
            ((Number) row[2]).longValue(),
            ((Number) row[3]).longValue(),
            ((Number) row[4]).longValue()
        );
    }

    @Override
    @Transactional
    public void suspendUser(Long userId) {
        var user = findUserOrThrow(userId);
        user.setActive(false);
        userRepository.save(user);
    }

    @Override
    @Transactional
    public void activateUser(Long userId) {
        var user = findUserOrThrow(userId);
        user.setActive(true);
        userRepository.save(user);
    }

    @Override
    @Transactional
    public String resetPassword(Long userId) {
        var user = findUserOrThrow(userId);
        var tempPassword = generateTempPassword();
        user.setPasswordHash(passwordEncoder.encode(tempPassword));
        userRepository.save(user);
        return tempPassword;
    }

    @Override
    @Transactional
    public void updateUserRole(Long userId, Role role) {
        var user = findUserOrThrow(userId);
        user.setRole(role);
        userRepository.save(user);
    }

    private User findUserOrThrow(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
    }

    private UserSummary toSummary(User user) {
        return new UserSummary(
                user.getId(),
                user.getFirstName(),
                user.getLastName(),
                user.getEmail(),
                user.getRole(),
                user.isActive(),
                user.getCreatedAt()
        );
    }

    private Specification<User> buildSearchSpec(String search) {
        return (root, query, cb) -> {
            if (search == null || search.isBlank()) return null;
            String pattern = SearchUtils.toLikeContainsPattern(search);
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.like(cb.lower(root.get("firstName")), pattern.toLowerCase()));
            predicates.add(cb.like(cb.lower(root.get("lastName")), pattern.toLowerCase()));
            predicates.add(cb.like(cb.lower(root.get("email")), pattern.toLowerCase()));
            return cb.or(predicates.toArray(new Predicate[0]));
        };
    }

    private String generateTempPassword() {
        char[] pw = new char[TEMP_PASSWORD_LENGTH];
        for (int i = 0; i < pw.length; i++) {
            pw[i] = ALL_CHARS.charAt(SECURE_RANDOM.nextInt(ALL_CHARS.length()));
        }
        pw[0] = LOWER.charAt(SECURE_RANDOM.nextInt(LOWER.length()));
        pw[1] = UPPER.charAt(SECURE_RANDOM.nextInt(UPPER.length()));
        pw[2] = DIGITS.charAt(SECURE_RANDOM.nextInt(DIGITS.length()));
        pw[3] = SPECIAL.charAt(SECURE_RANDOM.nextInt(SPECIAL.length()));
        shuffleInPlace(pw);
        return new String(pw);
    }

    private void shuffleInPlace(char[] arr) {
        for (int i = arr.length - 1; i > 0; i--) {
            int j = SECURE_RANDOM.nextInt(i + 1);
            char tmp = arr[i];
            arr[i] = arr[j];
            arr[j] = tmp;
        }
    }

    /**
     * H2 wraps multi-column aggregate results in a nested Object[].
     * Unwrap to a flat Object[] for consistent results across H2 and PostgreSQL.
     */
    private Object[] flattenResult(Object[] result) {
        return Stream.of(result)
                .filter(o -> result.length == 1 && o instanceof Object[])
                .map(Object[].class::cast)
                .findFirst()
                .orElse(result);
    }
}
