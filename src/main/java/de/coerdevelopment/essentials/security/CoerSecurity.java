package de.coerdevelopment.essentials.security;

import de.coerdevelopment.essentials.utils.TimeUtils;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.KeySpec;
import java.util.Base64;
import java.util.Date;
import java.util.Map;

public class CoerSecurity {
    
    private static CoerSecurity instance;
    
    public static CoerSecurity getInstance() {
        if (instance == null) {
            new CoerSecurity("SHA-256", 64, TimeUtils.getInstance().getMillisecondsFromDays(1));
        }
        return instance;
    }

    public static CoerSecurity newInstance(String algorithm, int saltLength, long tokenExpiration) {
        instance = new CoerSecurity(algorithm, saltLength, tokenExpiration);
        return instance;
    }

    // Hash settings
    private final String ALGORITHM;
    private final int SALT_LENGTH;

    // Token settings
    private final long TOKEN_EXPIRATION;
    private final SignatureAlgorithm SIGNATURE_ALGORITHM = SignatureAlgorithm.HS512;
    private final SecretKey SECRET_KEY = Keys.secretKeyFor(SIGNATURE_ALGORITHM);

    // Password settings
    private final int PASSWORD_ITERATIONS = 91826;
    private final int PASSWORD_KEY_LENGTH = 256;
    private final String PASSWORD_ALGORITHM = "PBKDF2WithHmacSHA256";

    public CoerSecurity(String algorithm, int saltLength, long tokenExpiration) {
        this.ALGORITHM = algorithm;
        this.SALT_LENGTH = saltLength;
        this.TOKEN_EXPIRATION = tokenExpiration;
        instance = this;
    }

    public String createToken(String subject, long expiration, Map<String, Object> claims) {
        Date now = new Date();
        Date expirationDate = new Date(now.getTime() + expiration);
        String token = Jwts.builder()
                .setSubject(subject)
                .setIssuedAt(now)
                .claims(claims)
                .setExpiration(expirationDate)
                .signWith(SIGNATURE_ALGORITHM, SECRET_KEY)
                .compact();
        return token;
    }

    public String createToken(String subject) {
        return createToken(subject, TOKEN_EXPIRATION, null);
    }

    public String createToken(String subject, Map<String, Object> claims) {
        return createToken(subject, TOKEN_EXPIRATION, claims);
    }

    public String createToken(int subject) {
        return createToken(String.valueOf(subject));
    }

    public String createToken(int subject, long expiration) {
        return createToken(String.valueOf(subject), expiration, null);
    }

    public String createToken(String subject, long expiration) {
        return createToken(String.valueOf(subject), expiration, null);
    }

    public Object getClaim(String token, String key) {
        try {
            if (!isTokenValid(token)) {
                throw new IllegalStateException("Token is invalid");
            }
            Claims claims = Jwts.parser().setSigningKey(SECRET_KEY).build().parseClaimsJws(token).getBody();
            return claims.get(key);
        } catch (Exception ex) {
            throw new IllegalStateException("Token is invalid");
        }
    }

    public String getSubjectFromToken(String token) {
        try {
            if (!isTokenValid(token)) {
                throw new IllegalStateException("Token is invalid");
            }
            Claims claims = Jwts.parser().setSigningKey(SECRET_KEY).build().parseClaimsJws(token).getBody();
            return claims.getSubject();
        } catch (Exception ex) {
            throw new IllegalStateException("Token is invalid");
        }
    }

    public int getSubjectFromTokenAsInt(String token) {
        return Integer.parseInt(getSubjectFromToken(token));
    }

    private boolean isTokenValid(String token) {
        try {
            Jwts.parser().setSigningKey(SECRET_KEY).build().parseSignedClaims(token);
            return true;
        } catch (Exception ex) {
            return false;
        }
    }

    public String generateSalt() {
        SecureRandom random = new SecureRandom();
        byte[] salt = new byte[SALT_LENGTH];
        random.nextBytes(salt);
        return Base64.getEncoder().encodeToString(salt).substring(0, SALT_LENGTH);
    }

    public String hashPassword(String password, String salt) {
        try {
            KeySpec spec = new PBEKeySpec(password.toCharArray(), salt.getBytes(), PASSWORD_ITERATIONS, PASSWORD_KEY_LENGTH);
            SecretKeyFactory factory = SecretKeyFactory.getInstance(PASSWORD_ALGORITHM);
            byte[] hash = factory.generateSecret(spec).getEncoded();
                return Base64.getEncoder().encodeToString(hash);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return null;
    }

    public synchronized String stringToHash(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance(ALGORITHM);
            byte[] array = md.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuffer sb = new StringBuffer();
            for (int i = 0; i < array.length; ++i) {
                sb.append(Integer.toHexString((array[i] & 0xFF) | 0x100).substring(1, 3));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return null;
    }

}
