package de.coerdevelopment.essentials;

import de.coerdevelopment.essentials.module.AccountModule;
import de.coerdevelopment.essentials.module.MailModule;
import de.coerdevelopment.essentials.module.ModuleType;
import de.coerdevelopment.essentials.module.SQLModule;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class Main {

    public static void main(String[] args) {
        SQLModule sqlModule = (SQLModule) CoerEssentials.getInstance().enableModule(ModuleType.SQL);
        sqlModule.initSQL();
        MailModule mailModule = (MailModule) CoerEssentials.getInstance().enableModule(ModuleType.MAIL);
        AccountModule accountModule = (AccountModule) CoerEssentials.getInstance().enableModule(ModuleType.ACCOUNT);

        SpringApplication app = new SpringApplication(Main.class);
        app.run(args);
    }

}
