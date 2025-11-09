package br.pucpr.authserver.users


import br.pucpr.authserver.roles.Role
import com.fasterxml.jackson.annotation.JsonIgnore
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.JoinTable
import jakarta.persistence.ManyToMany
import jakarta.persistence.Table
import jakarta.persistence.Transient
import java.time.LocalDateTime

@Entity
@Table(name = "TblUser")
class User(
    @Id @GeneratedValue
    var id: Long? = null,
    
    @Column(unique = true)
    var email: String = "",

    var password: String = "",

    var name: String = "",

    @Column(unique = true, nullable = true)
    var phone: String? = null, 

    @Column(name = "device_uuid", nullable = true)
    var deviceUuid: String? = null, 

    @Column(name = "confirmation_code", nullable = true)
    var confirmationCode: String? = null,

    @Column(name = "code_expiration", nullable = true)
    var codeExpiration: LocalDateTime? = null, 

    @Column(name = "is_active")
    var isActive: Boolean = false,

    @ManyToMany
    @JoinTable(
        name="UserRole",
        joinColumns = [JoinColumn(name = "idUser")],
        inverseJoinColumns = [JoinColumn(name = "idRole")]
    )
    val roles: MutableSet<Role> = mutableSetOf()
) {
    @get:JsonIgnore
    @get:Transient
    val isAdmin: Boolean get() = roles.any { it.name == "ADMIN" }
}