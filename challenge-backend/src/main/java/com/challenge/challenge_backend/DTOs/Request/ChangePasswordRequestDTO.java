package com.challenge.challenge_backend.DTOs.Request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO para cambio de contraseña.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChangePasswordRequestDTO {
    
    @NotBlank(message = "La contraseña actual es obligatoria")
    private String oldPassword;
    
    @NotBlank(message = "La nueva contraseña es obligatoria")
    @Size(min = 6, max = 100, message = "La nueva contraseña debe tener entre 6 y 100 caracteres")
    @Pattern(
        regexp = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z]).*$",
        message = "La nueva contraseña debe contener al menos un número, una minúscula y una mayúscula"
    )
    private String newPassword;
}
