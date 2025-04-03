package com.repo.auth.core.token.provider

import com.repo.auth.common.exception.AuthException.*
import com.repo.auth.common.exception.AuthException.AccessTokenException.*
import com.repo.auth.core.config.JwtConfig
import com.repo.auth.core.redis.enums.KeyType
import com.repo.auth.core.redis.service.RedisService
import com.repo.auth.core.token.enums.AccessTokenClaims.*
import com.repo.auth.core.token.extensions.getAccessTokenHeader
import com.repo.auth.domain.user.enums.UserRole
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import jakarta.servlet.http.HttpServletRequest
import org.springframework.stereotype.Component
import java.time.*
import java.util.*

@Component
class AccessTokenProvider(
    private val config: JwtConfig,
    private val redisService: RedisService,
) {

    private val key by lazy { Keys.hmacShaKeyFor(config.secret.toByteArray()) }

    fun generateAccessToken(
        userId: String,
        userRole: UserRole,
    ): String {
        val now = Date()
        val expiry = Date(now.time + config.expiration)

        return Jwts.builder()
            .subject(userId)
            .claim(ROLE.claim, userRole.role)
            .issuedAt(now)
            .expiration(expiry)
            .signWith(key)
            .compact()
    }

    fun extractToken(
        request: HttpServletRequest
    ): String {
        val header = request.getAccessTokenHeader()

        if (!header.startsWith("Bearer ")) {
            throw UnauthorizedException()
        }
        return header.removePrefix("Bearer ").trim()
    }

    fun validateToken(
        token: String,
        expectedRole: UserRole
    ): Boolean {
        val claims = this.getClaims(token)
        val role = UserRole.fromRole(claims[ROLE.claim] as? String)

        when {
            redisService.has(KeyType.BLACKLIST, token) -> throw InvalidAccessTokenException()
            claims.expiration.before(Date()) -> throw ExpiredAccessTokenException()
            role != expectedRole -> throw InvalidRoleException()
            else -> return true
        }
    }

    fun getIdByToken(
        token: String
    ): String =
        this.getClaims(token).subject

    fun saveBlackList(
        token: String
    ) =
        redisService.save(KeyType.BLACKLIST, token, "logout", this.getRemainingTime(token))

    fun getRemainingTime(
        token: String
    ): Duration =
        Duration.ofMillis(maxOf(0, this.getClaims(token).expiration.time - Date().time))

    private fun getClaims(
        token: String
    ) =
        runCatching {
            Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .payload
        }.getOrElse {
            throw InvalidAccessTokenException()
        }

}