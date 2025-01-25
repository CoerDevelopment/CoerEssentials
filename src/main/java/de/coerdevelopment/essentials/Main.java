package de.coerdevelopment.essentials;

import de.coerdevelopment.essentials.config.Config;
import de.coerdevelopment.essentials.module.ModuleType;
import de.coerdevelopment.essentials.module.SQLModule;

import java.util.logging.ConsoleHandler;
import java.util.logging.Handler;
import java.util.logging.LogManager;
import java.util.logging.SimpleFormatter;

public class Main {

    public static void main(String[] args) {
        SQLModule sqlModule = (SQLModule) CoerEssentials.getInstance().enableModule(ModuleType.SQL);
        sqlModule.initSQL();
    }

}
