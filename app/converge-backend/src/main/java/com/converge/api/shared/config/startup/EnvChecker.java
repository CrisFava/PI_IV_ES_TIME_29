package com.converge.api.shared.config.startup;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import static com.converge.api.shared.config.customization.Color.*;

@Slf4j
@Component
public class EnvChecker {

    public static boolean check(){

        boolean result = System.getenv("ENV_CHECK") != null;

        String msg = result  ? GREEN + "Environment variables loaded successfully."
                : RED + "Environment variables not found.";

        log.info("{}{}", msg, RESET);

        return result;
    }
}
