package com.application.gms.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;

@Setter
@Getter
@AllArgsConstructor
public class AuthenticationResponse {
    private String token;
    private String refreshToken;
    private String username;
    private Collection<? extends GrantedAuthority> authorities;
}
