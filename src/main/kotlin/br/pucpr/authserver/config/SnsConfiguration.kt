package br.pucpr.authserver.config

import br.pucpr.authserver.users.SmsService
import br.pucpr.authserver.users.MockSmsService
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.beans.factory.annotation.Value
import br.pucpr.authserver.users.AwsSnsSmsService

/**
 * Classe de configuração do Spring para o serviço de SMS.
 * Garante que o SmsService correto seja injetado (Mock ou AWS SNS).
 */
@Configuration
class SnsConfiguration {
    
    /**
     * Bean para o ambiente de PROD (ou qualquer profile onde 'prod' estiver ativo).
     * Injeta a implementação real do AWS SNS.
     */
    @Bean
    @Profile("!local") 
    fun awsSnsService(@Value("\${aws.region}") awsRegion: String): SmsService {
        println("Inicializando SmsService: AWS SNS na região $awsRegion")
        return AwsSnsSmsService(awsRegion)
    }

    /**
     * Bean de fallback para o ambiente de LOCAL, etc (quando 'prod' NÃO estiver ativo).
     * Injeta a implementação Mock que apenas imprime no console.
     */
    @Bean
    @Profile("local") 
    fun mockSmsService(): SmsService {
        println("Inicializando SmsService: MockService (Envio de SMS no console)")
        return MockSmsService()
    }
}