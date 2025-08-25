package de.coerdevelopment.essentials;

import de.coerdevelopment.essentials.module.*;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class CoerEssentialsTester {

    public static void main(String[] args) {
        SQLModule sqlModule = (SQLModule) CoerEssentials.getInstance().enableModule(ModuleType.SQL);
        sqlModule.initSQL();
        RedisModule redisModule = (RedisModule) CoerEssentials.getInstance().enableModule(ModuleType.REDIS);
        MailModule mailModule = (MailModule) CoerEssentials.getInstance().enableModule(ModuleType.MAIL);
        AccountModule accountModule = (AccountModule) CoerEssentials.getInstance().enableModule(ModuleType.ACCOUNT);

        CoerEssentials.getInstance().startExecutingJobs();
        SpringApplication app = new SpringApplication(CoerEssentialsTester.class);
        app.run(args);
    }

}
