package com.marakicode.financetracker.auth;

import com.marakicode.financetracker.common.ResourceNotFoundException;
import com.marakicode.financetracker.users.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserService userService;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        try {
            var user = userService.findByEmail(email);
            String roleName = user.getRole() != null ? user.getRole().name() : "USER";
            return new User(user.getEmail(), user.getPasswordHash(),
                    List.of(new SimpleGrantedAuthority("ROLE_" + roleName)));
        } catch (ResourceNotFoundException e) {
            throw new UsernameNotFoundException("User not found with email: " + email, e);
        }
    }
}
