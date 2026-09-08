package org.celimited.manager.data.remote.dto.login

import kotlinx.serialization.json.Json
import org.celimited.manager.core.network.ApiEnvelope
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class LoginResponseDtoTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `decodes a business failure response with null user and token fields`() {
        val body = """
            {
                "data": {
                    "user": {"userId": null, "fullName": null, "email": null, "phone": null, "profilePictureUrl": null, "role": null, "status": null},
                    "tokens": {"accessToken": null, "refreshToken": null, "tokenType": null, "expiresIn": 0, "refreshTokenExpiresIn": 0}
                },
                "success": false,
                "statusCode": 403,
                "message": "The user is already logged in with another device "
            }
        """.trimIndent()

        val envelope = json.decodeFromString<ApiEnvelope<LoginResponseDataDto>>(body)

        assertTrue(!envelope.success)
        assertEquals(403, envelope.statusCode)
        assertNull(envelope.data?.user?.userId)
    }

    @Test
    fun `decodes a successful login response`() {
        val body = """
            {
                "data": {
                    "user": {"userId": "SR0001", "fullName": "Md.Abu Daud", "email": "shapon@celimited.com", "phone": "01926633159", "profilePictureUrl": "", "role": "SR", "status": "Authorized"},
                    "tokens": {"accessToken": "token", "refreshToken": "refresh", "tokenType": null, "expiresIn": 299, "refreshTokenExpiresIn": 0}
                },
                "success": true,
                "statusCode": 200,
                "message": "Successfully Logged in"
            }
        """.trimIndent()

        val envelope = json.decodeFromString<ApiEnvelope<LoginResponseDataDto>>(body)

        assertTrue(envelope.success)
        assertEquals("SR0001", envelope.data?.user?.userId)
        assertEquals(299, envelope.data?.tokens?.expiresIn)
    }
}
