package com.itranswarp.web.controller;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.itranswarp.model.PasskeyAuth;
import com.itranswarp.model.PasskeyChallenge;
import com.itranswarp.service.UserService;
import com.itranswarp.web.controller.PasskeyApiController.RegisterBean;
import com.itranswarp.web.controller.PasskeyApiController.SigninBean;
import com.itranswarp.web.controller.PasskeyApiController.SigninBean.SigninResponseBean;
import com.itranswarp.web.controller.PasskeyApiController.RegisterBean.RegisterResponseBean;

public class PasskeyApiControllerTest {

    final long USER_ID = 123456789L;
    final long PK_AUTH_ID = 1020304050L;

    final String HOST = "passkeys-demo.appspot.com";
    final String ORIGIN = "https://" + HOST;

    final String STR_USER_ID = "M9mzHKG_2axsYPu3Vt5qQArXiWwghxRUJ2zZt06qXUg";
    final String STR_CREDENTIAL_ID = "jKeuFIcRgx5R1SRNkW44KZWy6c4";
    final String STR_PUBLIC_KEY = "pQECAyYgASFYIM2ljr006gKx5Y_C_DZSznNQkotRJijODzofxVGKmjztIlgg-qKgWchVfJAr1iVy-yoPWX5k6Bpl8uijWYtHkpnD7k4";

    final String STR_CHALLENGE_REG = "x1wRuShyI4k7BqYJi60kVk-clJWsPnBGgh_7z-W9QYk";
    final String STR_CHALLENGE_SIGNIN = "CGe2dPpiQugRqpAdmbkB9vSpXfQ5I-sQVm3jcMWAUwk";

    PasskeyApiController controller;

    @BeforeEach
    public void setUp() {
        UserService userService = new UserService() {
            @Override
            public PasskeyAuth fetchPasskeyAuthByCredentialId(long userId, String credentialId) {
                if (userId == USER_ID && STR_CREDENTIAL_ID.equals(credentialId)) {
                    return createPasskeyAuth();
                }
                throw new IllegalArgumentException("Bad mock data.");
            }

            @Override
            public PasskeyChallenge fetchNonExpiredPasskeyChallenge(String challenge) {
                if (STR_CHALLENGE_SIGNIN.equals(challenge)) {
                    var pkc = new PasskeyChallenge();
                    pkc.challenge = STR_CHALLENGE_SIGNIN;
                    return pkc;
                }
                throw new IllegalArgumentException("Bad mock data.");
            }

            PasskeyAuth createPasskeyAuth() {
                var pkAuth = new PasskeyAuth();
                pkAuth.alg = -7;
                pkAuth.credentialId = STR_CREDENTIAL_ID;
                pkAuth.device = "Desktop";
                pkAuth.id = PK_AUTH_ID;
                pkAuth.pubKey = STR_PUBLIC_KEY;
                pkAuth.transports = "internal,hybrid";
                pkAuth.userId = USER_ID;
                return pkAuth;
            }
        };
        controller = new PasskeyApiController() {
            @Override
            long decodeUserHandle(String userHandle) {
                if (STR_USER_ID.equals(userHandle)) {
                    return USER_ID;
                }
                throw new IllegalArgumentException("Bad mock data.");
            }
        };
        controller.userService = userService;
    }

    @Test
    void testCreate() {
        var client = new RegisterBean();
        client.id = client.rawId = STR_CREDENTIAL_ID;
        client.type = "public-key";
        client.authenticatorAttachment = "platform";
        client.response = new RegisterResponseBean();
        client.response.clientDataJSON = "eyJ0eXBlIjoid2ViYXV0aG4uY3JlYXRlIiwiY2hhbGxlbmdlIjoieDF3UnVTaHlJNGs3QnFZSmk2MGtWay1jbEpXc1BuQkdnaF83ei1XOVFZayIsIm9yaWdpbiI6Imh0dHBzOi8vcGFzc2tleXMtZGVtby5hcHBzcG90LmNvbSJ9";
        client.response.attestationObject = "o2NmbXRkbm9uZWdhdHRTdG10oGhhdXRoRGF0YViY4IRbxDfJvMMpCYRaKlNcTJ1od6qAZma0i_KWKviQ-mtZAAAAAAAAAAAAAAAAAAAAAAAAAAAAFIynrhSHEYMeUdUkTZFuOCmVsunOpQECAyYgASFYIM2ljr006gKx5Y_C_DZSznNQkotRJijODzofxVGKmjztIlgg-qKgWchVfJAr1iVy-yoPWX5k6Bpl8uijWYtHkpnD7k4";
        client.response.transports = new String[] { "internal", "hybrid" };
        String[] rs = controller.parsePublicKey(client, ORIGIN, HOST);
        String challenge = rs[0];
        String credentialId = rs[1];
        String pubKey = rs[2];
        assertEquals(STR_CHALLENGE_REG, challenge);
        assertEquals(STR_CREDENTIAL_ID, credentialId);
        assertEquals(STR_PUBLIC_KEY, pubKey);
    }

    @Test
    void testSignin() {
        var client = new SigninBean();
        client.id = client.rawId = STR_CREDENTIAL_ID;
        client.type = "public-key";
        client.response = new SigninResponseBean();
        client.response.clientDataJSON = "eyJ0eXBlIjoid2ViYXV0aG4uZ2V0IiwiY2hhbGxlbmdlIjoiQ0dlMmRQcGlRdWdScXBBZG1ia0I5dlNwWGZRNUktc1FWbTNqY01XQVV3ayIsIm9yaWdpbiI6Imh0dHBzOi8vcGFzc2tleXMtZGVtby5hcHBzcG90LmNvbSJ9";
        client.response.authenticatorData = "4IRbxDfJvMMpCYRaKlNcTJ1od6qAZma0i_KWKviQ-msZAAAAAA";
        client.response.signature = "MEYCIQChD3WdFu2oUyElYrw-Zbe-QS0vlr_UBawY49blnH_tDgIhAL-nUzV7NduxnhWUcYpvCe4LRqdRk6mpcQNFQO0ejUNS";
        client.response.userHandle = STR_USER_ID;
        PasskeyAuth pkAuth = controller.parseFromSignature(client, ORIGIN, HOST);
        assertEquals(USER_ID, pkAuth.userId);
    }
}
