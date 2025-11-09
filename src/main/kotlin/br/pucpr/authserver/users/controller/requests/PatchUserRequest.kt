package br.pucpr.authserver.users.controller.requests

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Email

data class PatchUserRequest(
    @field:NotBlank
    val name: String?,

    @field:Email(message = "Email inválido")
    val email: String?
)