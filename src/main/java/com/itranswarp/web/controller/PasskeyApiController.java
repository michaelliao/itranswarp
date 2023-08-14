package com.itranswarp.web.controller;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.itranswarp.bean.setting.Website;
import com.itranswarp.common.ApiException;
import com.itranswarp.enums.ApiError;
import com.itranswarp.enums.Role;
import com.itranswarp.model.PasskeyAuth;
import com.itranswarp.model.PasskeyChallenge;
import com.itranswarp.model.User;
import com.itranswarp.util.CookieUtil;
import com.itranswarp.util.HttpUtil;
import com.itranswarp.web.filter.HttpContext;
import com.itranswarp.web.support.RoleWith;
import com.webauthn4j.WebAuthnManager;
import com.webauthn4j.converter.util.ObjectConverter;
import com.webauthn4j.data.AuthenticationData;
import com.webauthn4j.data.AuthenticationRequest;
import com.webauthn4j.data.RegistrationData;
import com.webauthn4j.data.RegistrationRequest;
import com.webauthn4j.data.SignatureAlgorithm;
import com.webauthn4j.data.attestation.AttestationObject;
import com.webauthn4j.data.attestation.authenticator.AuthenticatorData;
import com.webauthn4j.data.attestation.authenticator.COSEKey;
import com.webauthn4j.data.attestation.authenticator.EC2COSEKey;
import com.webauthn4j.data.client.ClientDataType;
import com.webauthn4j.data.client.CollectedClientData;
import com.webauthn4j.data.extension.authenticator.AuthenticationExtensionAuthenticatorOutput;
import com.webauthn4j.data.extension.authenticator.ExtensionAuthenticatorOutput;
import com.webauthn4j.data.extension.authenticator.RegistrationExtensionAuthenticatorOutput;
import com.webauthn4j.util.Base64UrlUtil;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@RestController
@RequestMapping("/api/passkey")
public class PasskeyApiController extends AbstractController {

    final static int ALG = -7;

    ObjectConverter objectConverter = new ObjectConverter();
    WebAuthnManager webAuthnManager = WebAuthnManager.createNonStrictWebAuthnManager(objectConverter);

    @Value("${spring.signin.passkey.challenge-timeout}")
    Duration passKeyChallengeTimeout = Duration.ofMinutes(1);

    @Scheduled(fixedDelay = 600_000)
    public void scheduledCleanupExpiredChallenge() {
        logger.info("scheduled cleanup expired challenge...");
        this.userService.deleteExpiresPassKeyChallenge();
    }

    @GetMapping("/create/options")
    @RoleWith(Role.SUBSCRIBER)
    public RegisterOptionsBean registerOptions(HttpServletRequest request) {
        User user = HttpContext.getRequiredCurrentUser();
        // long -> byte[8] -> url-base64:
        String userId = Base64UrlUtil.encodeToString(longToBytes(user.id));
        String host = HttpUtil.getHostname(request);
        Website website = settingService.getWebsiteFromCache();
        List<PasskeyAuth> existPks = userService.getPasskeyAuths(user.id);
        var options = new RegisterOptionsBean();
        options.challenge = userService.createPasskeyChallenge().challenge;
        options.rp = Map.of("name", website.name, "id", host);
        options.user = Map.of("id", userId, "name", user.email, "displayName", user.name);
        options.timeout = passKeyChallengeTimeout.toMillis();
        if (!existPks.isEmpty()) {
            options.excludeCredentials = existPks.stream().map(pk -> {
                var c = new CredentialBean();
                c.id = pk.credentialId;
                c.transports = pk.transports.split("\\,");
                return c;
            }).toArray(CredentialBean[]::new);
        }
        return options;
    }

    @PostMapping("/register")
    @RoleWith(Role.SUBSCRIBER)
    public Map<String, Boolean> registerCreate(@RequestBody RegisterBean client, HttpServletRequest request) {
        User user = HttpContext.getRequiredCurrentUser();
        String[] rs = parsePublicKey(client, HttpUtil.getOrigin(request), HttpUtil.getHostname(request));
        String challenge = rs[0];
        String credentialId = rs[1];
        String pubKey = rs[2];
        PasskeyChallenge pkChallenge = userService.fetchNonExpiredPasskeyChallenge(challenge);
        if (pkChallenge == null) {
            throw new ApiException(ApiError.PARAMETER_INVALID, "clientDataJSON", "Invalid challenge in clientDataJSON.");
        }
        String device = HttpUtil.getDevice(request);
        userService.createPasskeyAuth(user, device, ALG, credentialId, pubKey, client.response.transports);
        return API_RESULT_TRUE;
    }

    String[] parsePublicKey(RegisterBean client, String origin, String rpId) {
        if (client.id == null || !client.id.equals(client.rawId)) {
            throw new ApiException(ApiError.PARAMETER_INVALID, "id", "Invalid id.");
        }

        // check type == "public-key":
        if (!"public-key".equals(client.type)) {
            throw new ApiException(ApiError.PARAMETER_INVALID, "type", "Invalid type.");
        }
        // check authenticator attachment == "platform":
        if (!"platform".equals(client.authenticatorAttachment)) {
            throw new ApiException(ApiError.PARAMETER_INVALID, "authenticatorAttachment", "Invalid authenticatorAttachment.");
        }
        if (client.response == null) {
            throw new ApiException(ApiError.PARAMETER_INVALID, "response", "Invalid response.");
        }
        var registrationRequest = new RegistrationRequest(Base64UrlUtil.decode(client.response.attestationObject),
                Base64UrlUtil.decode(client.response.clientDataJSON), Set.of(client.response.transports));
        RegistrationData registrationData = webAuthnManager.parse(registrationRequest);
        CollectedClientData collectedClientData = registrationData.getCollectedClientData();
        if (collectedClientData == null) {
            throw new ApiException(ApiError.PARAMETER_INVALID, "clientDataJSON", "Invalid clientDataJSON.");
        }

        // check type == "webauthn.create":
        checkType(collectedClientData, ClientDataType.WEBAUTHN_CREATE);

        // check origin:
        checkOrigin(collectedClientData, origin);

        // get challenge:
        String challenge = Base64UrlUtil.encodeToString(collectedClientData.getChallenge().getValue());

        // decode from attestation-object:
        AttestationObject attestationObject = registrationData.getAttestationObject();
        if (attestationObject == null) {
            throw new ApiException(ApiError.PARAMETER_INVALID, "attestationObject", "Invalid attestationObject.");
        }
        AuthenticatorData<RegistrationExtensionAuthenticatorOutput> authenticatorData = attestationObject.getAuthenticatorData();

        // check rpIdHash:
        checkRpIdHash(authenticatorData, rpId);

        var attestedCredentialData = authenticatorData.getAttestedCredentialData();
        var credentialId = attestedCredentialData.getCredentialId();
        var key = attestedCredentialData.getCOSEKey();
        if (key instanceof EC2COSEKey ec2Key) {
            byte[] keyBytes = objectConverter.getCborConverter().writeValueAsBytes(key);
            return new String[] { challenge, Base64UrlUtil.encodeToString(credentialId), Base64UrlUtil.encodeToString(keyBytes) };
        } else {
            throw new ApiException(ApiError.PARAMETER_INVALID, "attestationObject", "Invalid EdDSA key.");
        }
    }

    @GetMapping("/get/options")
    public GetOptionsBean signinParams(HttpServletRequest request) {
        var options = new GetOptionsBean();
        options.challenge = userService.createPasskeyChallenge().challenge;
        options.rpId = HttpUtil.getHostname(request);
        options.timeout = passKeyChallengeTimeout.toMillis();
        // NOTE: options.allowCredentials is always empty to
        // let browser select local credentials.
        return options;
    }

    @PostMapping("/signin")
    public Map<String, String> signinRequest(@RequestBody SigninBean client, HttpServletRequest request, HttpServletResponse response) {
        PasskeyAuth pkAuth = parseFromSignature(client, HttpUtil.getOrigin(request), HttpUtil.getHostname(request));
        this.userService.updatePasskeyLastUsed(pkAuth);
        // set cookie:
        long expires = System.currentTimeMillis() + 7 * 24 * 3600_000L;
        String cookieStr = CookieUtil.encodeSessionCookie(pkAuth, expires, encryptService.getSessionHmacKey());
        CookieUtil.setSessionCookie(request, response, cookieStr, (int) ((expires - System.currentTimeMillis()) / 1000));
        String referer = HttpUtil.getReferer(request);
        return Map.of("url", referer);
    }

    long decodeUserHandle(String userHandle) {
        return bytesToLong(Base64UrlUtil.decode(userHandle));
    }

    PasskeyAuth parseFromSignature(SigninBean client, String origin, String rpId) {
        if (client.id == null || !client.id.equals(client.rawId)) {
            throw new ApiException(ApiError.PARAMETER_INVALID, "id", "Invalid id.");
        }
        byte[] credentialId = Base64UrlUtil.decode(client.id);
        // check type == "public-key":
        if (!"public-key".equals(client.type)) {
            throw new ApiException(ApiError.PARAMETER_INVALID, "type", "Invalid type.");
        }
        if (client.response == null) {
            throw new ApiException(ApiError.PARAMETER_INVALID, "response", "Invalid response.");
        }
        // get user id:
        long userId = decodeUserHandle(client.response.userHandle);
        AuthenticationRequest authenticationRequest = new AuthenticationRequest(credentialId, Base64UrlUtil.decode(client.response.authenticatorData),
                Base64UrlUtil.decode(client.response.clientDataJSON), Base64UrlUtil.decode(client.response.signature));

        AuthenticationData authenticationData = webAuthnManager.parse(authenticationRequest);
        CollectedClientData collectedClientData = authenticationData.getCollectedClientData();

        // check clientDataJSON.type == 'webauthn.get':
        checkType(collectedClientData, ClientDataType.WEBAUTHN_GET);

        // check origin:
        checkOrigin(collectedClientData, origin);

        // try get non-expired challenge from db:
        byte[] challenge = collectedClientData.getChallenge().getValue();
        PasskeyChallenge pkChallenge = userService.fetchNonExpiredPasskeyChallenge(Base64UrlUtil.encodeToString(challenge));
        if (pkChallenge == null) {
            throw new ApiException(ApiError.PARAMETER_INVALID, "clientDataJSON", "Invalid challenge in clientDataJSON.");
        }
        AuthenticatorData<AuthenticationExtensionAuthenticatorOutput> authenticatorData = authenticationData.getAuthenticatorData();

        // check rpIdHash:
        checkRpIdHash(authenticatorData, rpId);

        // check signature:
        PasskeyAuth pkAuth = userService.fetchPasskeyAuthByCredentialId(userId, client.id);
        COSEKey key = objectConverter.getCborConverter().readValue(Base64UrlUtil.decode(pkAuth.pubKey), COSEKey.class);
        if (key instanceof EC2COSEKey ec2Key) {
            // verify signature:
            byte[] rawAuthenticatorData = authenticationData.getAuthenticatorDataBytes();
            byte[] clientDataHash = authenticationData.getClientDataHash();
            byte[] signedData = ByteBuffer.allocate(rawAuthenticatorData.length + clientDataHash.length).put(rawAuthenticatorData).put(clientDataHash).array();
            byte[] signature = authenticationData.getSignature();
            if (!verifySignature(ec2Key, signature, signedData)) {
                throw new ApiException(ApiError.PARAMETER_INVALID, "signature", "Verify signature failed.");
            }
            // check userId:
            if (userId != pkAuth.userId) {
                throw new ApiException(ApiError.AUTH_SIGNIN_FAILED, "userHandle", "Inconsistant user id in PasskeyAuth.");
            }
            // check credentialId:
            if (!pkAuth.credentialId.equals(Base64UrlUtil.encodeToString(credentialId))) {
                throw new ApiException(ApiError.AUTH_SIGNIN_FAILED, "credentialId", "Inconsistant credentialId in PasskeyAuth.");
            }
            return pkAuth;
        } else {
            throw new ApiException(ApiError.PARAMETER_INVALID, "attestationObject", "Invalid EdDSA key.");
        }
    }

    // Util ///////////////////////////////////////////////////////////////////

    static byte[] longToBytes(long l) {
        byte[] result = new byte[8];
        for (int i = 7; i >= 0; i--) {
            result[i] = (byte) (l & 0xFF);
            l >>= 8;
        }
        return result;
    }

    static long bytesToLong(final byte[] b) {
        if (b.length != 8) {
            throw new IllegalArgumentException("Invalid bytes.");
        }
        long result = 0;
        for (int i = 0; i < 8; i++) {
            result <<= 8;
            result |= (b[i] & 0xFF);
        }
        return result;
    }

    void checkType(CollectedClientData collectedClientData, ClientDataType expectedClientDataType) {
        if (!Objects.equals(collectedClientData.getType(), expectedClientDataType)) {
            throw new ApiException(ApiError.PARAMETER_INVALID, "clientDataJSON", "Invalid type in clientDataJSON.");
        }
    }

    void checkOrigin(CollectedClientData collectedClientData, String expectedOrigin) {
        if (!expectedOrigin.equals(collectedClientData.getOrigin().toString())) {
            throw new ApiException(ApiError.PARAMETER_INVALID, "clientDataJSON", "Invalid origin in clientDataJSON.");
        }
    }

    void checkRpIdHash(AuthenticatorData<? extends ExtensionAuthenticatorOutput> authenticatorData, String rpId) {
        // check rpIdHash:
        byte[] rpIdHash = authenticatorData.getRpIdHash();
        MessageDigest md;
        try {
            md = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
        byte[] expectedRpIdHash = md.digest(rpId.getBytes(StandardCharsets.UTF_8));
        if (!Arrays.equals(rpIdHash, expectedRpIdHash)) {
            throw new ApiException(ApiError.PARAMETER_INVALID, "authenticatorData", "Invalid rpIdHash in authenticatorData.");
        }
    }

    boolean verifySignature(EC2COSEKey coseKey, byte[] signature, byte[] data) throws IllegalArgumentException {
        try {
            PublicKey publicKey = coseKey.getPublicKey();
            SignatureAlgorithm signatureAlgorithm = coseKey.getAlgorithm().toSignatureAlgorithm();
            String jcaName = signatureAlgorithm.getJcaName();
            Signature verifier = Signature.getInstance(jcaName);
            verifier.initVerify(publicKey);
            verifier.update(data);
            return verifier.verify(signature);
        } catch (NoSuchAlgorithmException | SignatureException | InvalidKeyException | RuntimeException e) {
            throw new ApiException(ApiError.PARAMETER_INVALID, "signature", "Invalid signature in authenticatorData");
        }
    }

    // JSON Object ////////////////////////////////////////////////////////////

    /**
     * Sample request:
     * 
     * <code>
     * {
     *     "challenge": "JhkTxPB-pmHsp0SfxArcXJY82mG0kuDr02qWwNKW_tw",
     *     "rp": {
     *         "name": "Passkeys Demo",
     *         "id": "passkeys-demo.appspot.com"
     *     },
     *     "user": {
     *         "id": "GOVsRuhMQWNoScmh_cK02QyQwTolHSUSlX5ciH242Y4",
     *         "name": "cryptomichael",
     *         "displayName": "Crypto Michael"
     *     },
     *     "pubKeyCredParams": [
     *         {
     *             "alg": -7,
     *             "type": "public-key"
     *         }
     *     ],
     *     "timeout": 60000,
     *     "attestation": "none",
     *     "excludeCredentials": [],
     *     "authenticatorSelection": {
     *         "authenticatorAttachment": "platform",
     *         "requireResidentKey": true,
     *         "residentKey": "required"
     *     },
     *     "extensions": {
     *         "credProps": true
     *     }
     * }
     * </code>
     */
    public static class RegisterOptionsBean {
        private static final CredentialBean[] EMPTY = new CredentialBean[0];

        public String challenge;
        public Map<String, String> rp;
        public Map<String, String> user;
        public PubKeyCredParamsBean[] pubKeyCredParams = PubKeyCredParamsBean.PARAMS;
        public long timeout;
        public String attestation = "none";
        public CredentialBean[] excludeCredentials = EMPTY;
        public Map<String, Object> authenticatorSelection = Map.of( //
                "authenticatorAttachment", "platform", //
                "requireResidentKey", true, //
                "residentKey", "required");
        public Map<String, Object> extensions = Map.of("credProps", true);
    }

    /**
     * Sample request:
     * 
     * <code>
     * {
     *     "challenge": "VGpgt3_P7tP9KEsM6k5B8jooBkT312VCW6mdT3etJxQzMYRu5zBBcTcv9RLP7BlHLyg9uQbp83Q55T94gBdKaw",
     *     "timeout": 60000,
     *     "rpId": "webauthn.io",
     *     "allowCredentials": [
     *         {
     *             "id": "Q4Xu_ZtVvHxMH4mM2MY474JML2M",
     *             "type": "public-key",
     *             "transports": [
     *                 "internal",
     *                 "hybrid"
     *             ]
     *         }
     *     ]
     * }
     * </code>
     */
    public static class GetOptionsBean {
        private static final CredentialBean[] EMPTY = new CredentialBean[0];

        public String challenge;
        public String rpId;
        public long timeout;
        public CredentialBean[] allowCredentials = EMPTY;
    }

    /**
     * <code>
     * {
     *     "id": "haVnqkfubvLDb8Na7f64b_saWs8",
     *     "rawId": "haVnqkfubvLDb8Na7f64b_saWs8",
     *     "type": "public-key",
     *     "response": {
     *         "clientDataJSON": "eyJ0eXBlIjoid2ViYXV0aG4uZ2V0IiwiY2hhbGxlbmdlIjoiUHpLLXhleDBvOGpabHZ2YVd3TmtnMGdfSUNMSHo4M2xscGd0cjh1NzJTWSIsIm9yaWdpbiI6Imh0dHBzOi8vcGFzc2tleXMtZGVtby5hcHBzcG90LmNvbSJ9",
     *         "authenticatorData": "4IRbxDfJvMMpCYRaKlNcTJ1od6qAZma0i_KWKviQ-msZAAAAAA"
     *     }
     * }
     * </code>
     */
    public static class RegisterBean {
        public String id;
        public String rawId;
        public String type;
        public String authenticatorAttachment;
        public RegisterResponseBean response;

        public static class RegisterResponseBean {
            public String clientDataJSON;
            public String attestationObject;
            public String[] transports;
        }
    }

    /**
     * <code>
     * {
     *     "id": "haVnqkfubvLDb8Na7f64b_saWs8",
     *     "rawId": "haVnqkfubvLDb8Na7f64b_saWs8",
     *     "type": "public-key",
     *     "response": {
     *         "clientDataJSON": "eyJ0eXBlIjoid2ViYXV0aG4uZ2V0IiwiY2hhbGxlbmdlIjoiUHpLLXhleDBvOGpabHZ2YVd3TmtnMGdfSUNMSHo4M2xscGd0cjh1NzJTWSIsIm9yaWdpbiI6Imh0dHBzOi8vcGFzc2tleXMtZGVtby5hcHBzcG90LmNvbSJ9",
     *         "authenticatorData": "4IRbxDfJvMMpCYRaKlNcTJ1od6qAZma0i_KWKviQ-msZAAAAAA",
     *         "signature": "MEUCIQDjvk29ZnKhQi4HnJrbFOK04NvL8i56J4Rqbbxvfprk-wIgO294Mtg-41FmryRKBL4QePIa1ns6VLxgiv03TcPAcaw",
     *         "userHandle": "Y3J5cHRvbWljaGFlbA"
     *     }
     * }
     * </code>
     */
    public static class SigninBean {
        public String id;
        public String rawId;
        public String type;
        public SigninResponseBean response;

        public static class SigninResponseBean {
            public String clientDataJSON;
            public String authenticatorData;
            public String signature;
            public String userHandle;
        }
    }

    public static class PubKeyCredParamsBean {
        public final int alg = ALG;
        public final String type = "public-key";

        static final PubKeyCredParamsBean[] PARAMS = new PubKeyCredParamsBean[] { new PubKeyCredParamsBean() };
    }

    public static class CredentialBean {
        public String id;
        public String type = "public-key";
        public String[] transports;
    }
}
