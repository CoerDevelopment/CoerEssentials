package de.coerdevelopment.essentials;

import de.coerdevelopment.essentials.module.AccountModule;
import de.coerdevelopment.essentials.module.MailModule;
import de.coerdevelopment.essentials.module.ModuleType;
import de.coerdevelopment.essentials.module.SQLModule;
import de.coerdevelopment.essentials.security.CoerSecurity;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class CoerEssentialsTester {

    public static void main(String[] args) {
        long start = System.currentTimeMillis();
        CoerSecurity.getInstance().hashPassword("password", CoerSecurity.getInstance().generateSalt());
        System.out.println("Time: " + (System.currentTimeMillis() - start) + "ms");
        if (true) {
            SQLModule sqlModule = (SQLModule) CoerEssentials.getInstance().enableModule(ModuleType.SQL);
            sqlModule.initSQL();
            MailModule mailModule = (MailModule) CoerEssentials.getInstance().enableModule(ModuleType.MAIL);
            AccountModule accountModule = (AccountModule) CoerEssentials.getInstance().enableModule(ModuleType.ACCOUNT);

            SpringApplication app = new SpringApplication(CoerEssentialsTester.class);
            app.run(args);
        }

    }

}
