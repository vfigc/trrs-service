package com.visionfund.trrs.security;

import java.util.Map;

/**
 * Expected shape of the platform-shared internal-auth HMAC secret in Secrets Manager:
 * {algorithm, activeKid, previousKid, keys}. keys maps a key id to its base64-encoded HMAC key
 * material - every entry in it is a currently-valid verification key, not just activeKid/
 * previousKid, so a token signed with any of them verifies successfully. activeKid/previousKid
 * describe intent on the signing side (integration-layer); this service only verifies, so it
 * simply trusts whatever kid the token names as long as that kid is present in keys.
 */
public record InternalAuthSecret(String algorithm, String activeKid, String previousKid, Map<String, String> keys) {
}
