package com.converge.api.shared.config.startup;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import static com.converge.api.shared.config.customization.Color.*;

@Slf4j
@Component
public class Setup {

    @PostConstruct
    public void init(){

        boolean isLoadedEnvVars = EnvChecker.check();

        if (isLoadedEnvVars){
            log.info("{}Converge API Initialized Successfully.{}", GREEN, RESET);
            return;
        }

        log.info("{}Converge API Initialized With Errors.{}", ORANGE, RESET);



    }
}
