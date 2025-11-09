package br.pucpr.authserver.users

import org.springframework.stereotype.Service

/**
 * Interface para abstrair a lógica de envio de SMS.
 * A implementação real (ex: AwsSnsSmsService) será injetada.
 */
interface SmsService {

    /**
     * Envia o código de confirmação via SMS.
     * @param phone O telefone para onde enviar.
     * @param code O código de confirmação gerado.
     * @return true se o envio foi bem-sucedido. Lança uma exceção em caso de falha.
     */
    fun sendConfirmationSms(phone: String, code: String): Boolean

    fun generateCode(): String {
        return (100000..999999).random().toString()
    }
}

class MockSmsService : SmsService {
    override fun sendConfirmationSms(phone: String, code: String): Boolean {
        println("--- SIMULAÇÃO DE ENVIO SMS ---")
        println("Para: ${phone}") 
        println("Código: $code")
        println("-----------------------------")
        
        return true 
    }
}