package com.application.gms.dto.request;

import com.application.gms.model.RoleEnum;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class RegistrationRequest {
    @NotBlank
    private String username;
    @NotBlank
    private String password;

    @Enumerated(EnumType.STRING)
    private RoleEnum roleEnum;
}
