package com.c05.kaz.ecommercebackend.security;

import com.c05.kaz.ecommercebackend.entity.UserAccount;
import com.c05.kaz.ecommercebackend.repository.UserAccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserAccountRepository userAccountRepository;

    @Override
    public UserDetails loadUserByUsername(String input) throws UsernameNotFoundException {
        UserAccount user = userAccountRepository.findByUsernameOrEmail(input, input)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + input));

        var authorities = user.getRoles().stream()
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role.getCode()))
                .collect(Collectors.toSet());

        // Cho phép Spring Security authenticate, còn status xử lý ở AuthController
        return new org.springframework.security.core.userdetails.User(
                user.getUsername(),
                user.getPassword(),
                true,   // enabled
                true,   // accountNonExpired
                true,   // credentialsNonExpired
                true,   // accountNonLocked
                authorities
        );
    }
}
