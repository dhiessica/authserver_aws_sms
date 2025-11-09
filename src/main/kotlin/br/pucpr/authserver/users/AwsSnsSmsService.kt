package br.pucpr.authserver.users 

import com.amazonaws.services.sns.AmazonSNS
import com.amazonaws.services.sns.AmazonSNSClientBuilder
import com.amazonaws.services.sns.model.PublishRequest
import com.amazonaws.services.sns.model.MessageAttributeValue
import com.amazonaws.regions.Regions
import java.util.logging.Logger

/**
 * Implementação concreta do SmsService utilizando o AWS Simple Notification Service (SNS).
 */
class AwsSnsSmsService(private val region: String) : SmsService {

    private val logger = Logger.getLogger(AwsSnsSmsService::class.java.name)
    private val regionConverted = Regions.fromName(region)

    private val snsClient: AmazonSNS = AmazonSNSClientBuilder.standard()
        .withRegion(regionConverted)
        .build()

    /**
     * Envia o código de confirmação via SMS usando a API do AWS SNS.
     * @param phone O telefone para onde enviar. (Assinatura Atualizada)
     * @param code O código de confirmação gerado.
     * @return true se o envio foi bem-sucedido, false caso contrário.
     */
    override fun sendConfirmationSms(phone: String, code: String): Boolean {
        val formattedPhoneNumber = formatPhoneNumber(phone)

        val message = "Seu código de login de confirmação é: $code. Este código expira em 5 minutos."

        try {
            val request = PublishRequest()
                .withMessage(message)
                .withPhoneNumber(formattedPhoneNumber)
                .addMessageAttributesEntry("AWS.SNS.SMS.SMSType", MessageAttributeValue()
                    .withDataType("String")
                    .withStringValue("Transactional"))

            val result = snsClient.publish(request)
            logger.info("SMS enviado com sucesso via SNS. MessageId: ${result.messageId}")
            return true

        } catch (e: Exception) {
            logger.severe("Erro ao enviar SMS via AWS SNS para $phone: ${e.message}")
            return false
        }
    }


    private fun formatPhoneNumber(phone: String): String {
        val cleanPhone = phone.replace("\\D".toRegex(), "")
        
        if (phone.startsWith("+")) {
            return phone
        }
        
        if (cleanPhone.length == 10 || cleanPhone.length == 11) {
             return "+55$cleanPhone"
        }

        return cleanPhone
    }
}