package br.pucpr.authserver.users.controller.requests

import jakarta.validation.constraints.NotBlank

data class ConfirmationRequest(
    @NotBlank
    val phone: String?,

    @NotBlank
    val uuid: String?,
    
    @NotBlank
    val confirmationCode: String?
)