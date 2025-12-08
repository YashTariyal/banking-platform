package com.banking.card.config;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "card.security.jwt")
public class JwtKeyProperties {

    /**
     * Optional list of symmetric keys identified by kid for rotation.
     * Each secret should be base64 encoded (consistent with spring secret-key usage).
     */
    private List<Key> keys = List.of();

    public List<Key> getKeys() {
        return keys;
    }

    public void setKeys(List<Key> keys) {
        this.keys = keys;
    }

    public static class Key {
        /**
         * The key id used in JWT header (kid).
         */
        private String kid;
        /**
         * Base64-encoded symmetric secret.
         */
        private String secret;

        public String getKid() {
            return kid;
        }

        public void setKid(String kid) {
            this.kid = kid;
        }

        public String getSecret() {
            return secret;
        }

        public void setSecret(String secret) {
            this.secret = secret;
        }
    }
}


