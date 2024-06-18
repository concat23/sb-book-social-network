package com.dev.sbbooknetwork;

import jakarta.xml.bind.DatatypeConverter;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Slf4j
class SbBookNetworkApplicationTests {

    @Test
    void contextLoads() {
    }

    private SecretKey generateAESKey() throws NoSuchAlgorithmException {
        KeyGenerator keyGen = KeyGenerator.getInstance("AES");
        keyGen.init(256);
        return keyGen.generateKey();
    }
    @Test
    void hash() throws NoSuchAlgorithmException {

        String password = "12345678910";

        MessageDigest md = MessageDigest.getInstance("MD5");

        md.update(password.getBytes());

        byte[] digest = md.digest();

        String md5Hash = DatatypeConverter.printHexBinary(digest);

        log.info("MD5 round 1: {}", md5Hash);

        md.update(password.getBytes());

        digest = md.digest();

        md5Hash = DatatypeConverter.printHexBinary(digest);

        log.info("MD5 round 2: {}", md5Hash);

        PasswordEncoder passwordEncoder = new BCryptPasswordEncoder(16);

        log.info("BCrypt round 1: {}", passwordEncoder.encode(password));

        log.info("BCrypt round 2: {}",passwordEncoder.encode(password) );

        SecretKey aesKey = generateAESKey();
        assertNotNull(aesKey);
        assertEquals("AES", aesKey.getAlgorithm());
        log.info("Generated AES key: {}", DatatypeConverter.printHexBinary(aesKey.getEncoded()));


    }


}


