package com.challenge.challenge_backend.DTOs.Request;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BatchClienteRequestDTO {
    
    @NotNull(message = "La lista de clientes no puede ser null")
    @Size(min = 1, max = 100, message = "Debe enviar entre 1 y 100 clientes")
    @Valid
    private List<ClienteRequestDTO> clientes;
}
