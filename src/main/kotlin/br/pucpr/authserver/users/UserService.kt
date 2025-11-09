package br.pucpr.authserver.users

import br.pucpr.authserver.exception.BadRequestException
import br.pucpr.authserver.exception.NotFoundException
import br.pucpr.authserver.roles.RoleRepository
import br.pucpr.authserver.security.Jwt
import br.pucpr.authserver.users.controller.requests.ConfirmationRequest
import br.pucpr.authserver.users.controller.requests.LoginRequest
import br.pucpr.authserver.users.controller.responses.LoginResponse
import br.pucpr.authserver.users.controller.responses.UserResponse
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import kotlin.jvm.optionals.getOrNull

@Service
class UserService(
    val repository: UserRepository,
    val roleRepository: RoleRepository,
    val jwt: Jwt,
    val smsService: SmsService
) {
    private val EXPIRATION_MINUTES = 5L
    
    fun insert(user: User): User {
        if (user.email.isNotEmpty() && repository.findByEmail(user.email) != null) {
            throw BadRequestException("User already exists with this email")
        }
        if (user.phone != null && repository.findByPhone(user.phone!!) != null) {
            throw BadRequestException("User already exists with this phone number")
        }
        return repository.save(user)
            .also { log.info("User inserted: {}", it.id) }
    }

    fun update(id: Long, name: String?): User? {
        val user = findByIdOrThrow(id)
        
        var changed = false
        if (name != null && user.name != name) {
            user.name = name
            changed = true
        }
        
        return if (changed) {
            repository.save(user)
        } else {
            null
        }
    }

    fun findAll(dir: SortDir = SortDir.ASC): List<User> = when (dir) {
        SortDir.ASC -> repository.findAll(Sort.by("name").ascending())
        SortDir.DESC -> repository.findAll(Sort.by("name").descending())
    }

    fun findByRole(role: String): List<User> = repository.findByRole(role)

    fun findByIdOrNull(id: Long) = repository.findById(id).getOrNull()
    private fun findByIdOrThrow(id: Long) =
        findByIdOrNull(id) ?: throw NotFoundException(id)

    fun delete(id: Long): Boolean {
        val user = findByIdOrNull(id) ?: return false
        if (user.roles.any { it.name == "ADMIN" }) {
            val count = repository.findByRole("ADMIN").size
            if (count == 1) throw BadRequestException("Cannot delete the last system admin!")
        }
        repository.delete(user)
        log.info("User deleted: {}", user.id)
        return true
    }

    fun addRole(id: Long, roleName: String): Boolean {
        val user = findByIdOrThrow(id)
        if (user.roles.any { it.name == roleName }) return false

        val role = roleRepository.findByName(roleName) ?:
            throw BadRequestException("Invalid role: $roleName")

        user.roles.add(role)
        repository.save(user)
        log.info("Granted role {} to user {}", role.name, user.id)
        return true
    }
    
    // --- NOVO FLUXO DE LOGIN POR TELEFONE ---
    /**
     * Implementa a lógica do POST /users/login (Telefone/UUID).
     * @return LoginResponse se for Login Direto (200 OK), ou null para Confirmação (202 Accepted).
     */
    fun login(request: LoginRequest): LoginResponse? {
        val phone = request.phone!!
        val uuid = request.uuid!!
        
        val existingUser = repository.findByPhone(phone)
        
        if (existingUser != null && existingUser.deviceUuid == uuid && existingUser.isActive) {
            log.info("Login bem-sucedido por Telefone/UUID para usuário: {}", existingUser.id)
            return LoginResponse(
                token = jwt.createToken(existingUser),
                user = UserResponse(existingUser)
            )
        }

        val userToConfirm = existingUser ?: User(
            id = null,
            email = "",
            password = "",
            name = "",
            phone = phone,
            isActive = false 
        )
        
        val confirmationCode = smsService.generateCode()
        val expirationTime = LocalDateTime.now().plusMinutes(EXPIRATION_MINUTES)
        
        userToConfirm.confirmationCode = confirmationCode
        userToConfirm.codeExpiration = expirationTime
        userToConfirm.deviceUuid = uuid 
        userToConfirm.isActive = false 

        val savedUser = repository.save(userToConfirm)

        if (!smsService.sendConfirmationSms(savedUser.phone!!, confirmationCode)) {
            throw BadRequestException("Falha ao enviar o código de confirmação via SMS.")
        }
        
        log.info("Usuário {} requer confirmação. Código enviado.", savedUser.id)
        return null 
    }
    
    /**
     * Implementa a lógica do POST /users/confirm.
     *
     * @return LoginResponse após confirmação bem-sucedida.
     */
    fun confirm(request: ConfirmationRequest): LoginResponse {
        val phone = request.phone!!
        val uuid = request.uuid!!
        val code = request.confirmationCode!!
        
        val user = repository.findByPhone(phone)
        
        if (user == null || user.deviceUuid != uuid || user.confirmationCode == null) {
            throw NotFoundException("Usuário ou código de confirmação inválido (404 Not Found)")
        }

        if (user.confirmationCode != code) {
            throw BadRequestException("Código de confirmação incorreto.")
        }

        if (user.codeExpiration != null && LocalDateTime.now().isAfter(user.codeExpiration)) {
            user.confirmationCode = null
            user.codeExpiration = null
            repository.save(user)
            throw BadRequestException("O código de confirmação expirou.")
        }

        user.isActive = true
        user.confirmationCode = null 
        user.codeExpiration = null
        user.deviceUuid = uuid 
        
        val confirmedUser = repository.save(user)
        log.info("Confirmação de usuário {} bem-sucedida.", confirmedUser.id)

        return LoginResponse(
            token = jwt.createToken(confirmedUser),
            user = UserResponse(confirmedUser)
        )
    }

    companion object {
        private val log = LoggerFactory.getLogger(UserService::class.java)
    }
}