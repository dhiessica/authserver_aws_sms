package br.pucpr.authserver.users.controller.requests

import jakarta.validation.constraints.NotBlank

data class LoginRequest(
    @NotBlank
    val phone: String?,
    
    @NotBlank
    val uuid: String?
)