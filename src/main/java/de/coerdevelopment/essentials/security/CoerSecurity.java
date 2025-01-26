package de.coerdevelopment.essentials.security;

import de.coerdevelopment.essentials.utils.TimeUtils;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Date;

public class CoerSecurity {
    
    private static CoerSecurity instance;
    
    public static CoerSecurity getInstance() {
        if (instance == null) {
            new CoerSecurity("SHA-256", 16, TimeUtils.getInstance().getMillisecondsFromDays(1));
        }
        return instance;
    }

    public static CoerSecurity newInstance(String algorithm, int saltLength, long tokenExpiration) {
        instance = new CoerSecurity(algorithm, saltLength, tokenExpiration);
        return instance;
    }

    private final String ALGORITHM;
    private final int SALT_LENGTH;
    private final long TOKEN_EXPIRATION;
    private final SignatureAlgorithm SIGNATURE_ALGORITHM = SignatureAlgorithm.HS512;
    private final SecretKey SECRET_KEY = Keys.secretKeyFor(SIGNATURE_ALGORITHM);

    public CoerSecurity(String algorithm, int saltLength, long tokenExpiration) {
        this.ALGORITHM = algorithm;
        this.SALT_LENGTH = saltLength;
        this.TOKEN_EXPIRATION = tokenExpiration;
        instance = this;
    }

    public String createToken(String subject, long expiration) {
        Date now = new Date();
        Date expirationDate = new Date(now.getTime() + expiration);
        String token = Jwts.builder()
                .setSubject(subject)
                .setIssuedAt(now)
                .setExpiration(expirationDate)
                .signWith(SIGNATURE_ALGORITHM, SECRET_KEY)
                .compact();
        return token;
    }

    public String createToken(String subject) {
        return createToken(subject, TOKEN_EXPIRATION);
    }

    public String createToken(int subject) {
        return createToken(String.valueOf(subject));
    }

    public String createToken(int subject, long expiration) {
        return createToken(String.valueOf(subject), expiration);
    }

    public String getStringFromToken(String token) {
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

    public int getIntFromToken(String token) {
        return Integer.parseInt(getStringFromToken(token));
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
