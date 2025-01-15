package de.coerdevelopment.essentials.test;

import de.coerdevelopment.essentials.security.CoerSecurity;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Random;

public class CoerSecurityTest {

    @Test
    public void testCreateToken() {
        Random random = new Random();
        int randomSubject = random.nextInt(10000);
        String token = CoerSecurity.getInstance().createToken(randomSubject);
        Assertions.assertNotNull(token, "Unable to generate a token");
    }

    @Test
    public void testGetSubjectFromToken() {
        Random random = new Random();
        int randomSubject = random.nextInt(10000);
        String token = CoerSecurity.getInstance().createToken(randomSubject);
        int subject = CoerSecurity.getInstance().getIntFromToken(token);
        Assertions.assertEquals(randomSubject, subject, "Subject does not match");
    }

    @Test
    public void testGenerateSalt() {
        String salt = CoerSecurity.getInstance().generateSalt();
        Assertions.assertNotNull(salt, "Unable to generate salt");
    }

    @Test
    public void testHashString() {
        String hashedString = CoerSecurity.getInstance().stringToHash("Test1234");
        Assertions.assertNotNull(hashedString, "Unable to hash a string");
    }

}
