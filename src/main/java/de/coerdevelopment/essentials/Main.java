package de.coerdevelopment.essentials;

import de.coerdevelopment.essentials.config.Config;
import de.coerdevelopment.essentials.module.AccountModule;
import de.coerdevelopment.essentials.module.MailModule;
import de.coerdevelopment.essentials.module.ModuleType;
import de.coerdevelopment.essentials.module.SQLModule;

public class Main {

    public static void main(String[] args) {
        SQLModule sqlModule = (SQLModule) CoerEssentials.getInstance().enableModule(ModuleType.SQL);
        sqlModule.initSQL();
        MailModule mailModule = (MailModule) CoerEssentials.getInstance().enableModule(ModuleType.MAIL);
        AccountModule accountModule = (AccountModule) CoerEssentials.getInstance().enableModule(ModuleType.ACCOUNT);
        accountModule.sendMailVerification(1);
    }

}
