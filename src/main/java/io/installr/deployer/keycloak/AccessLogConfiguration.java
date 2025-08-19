/*-
 * ---license-start
 * Installr Deployer Interface for keycloak-config-cli
 * ---
 * Copyright (C) 2025 Installr.io - maintained by Dorian Vallant (dorian@installr.io)
 * ---
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * ---license-end
 */
package io.installr.deployer.keycloak;

import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import ch.qos.logback.access.tomcat.LogbackValve;

@Configuration
public class AccessLogConfiguration {

    @Bean
    public TomcatServletWebServerFactory tomcatServletWebServerFactory() {
        final TomcatServletWebServerFactory tomcatServletWebServerFactory = new TomcatServletWebServerFactory();
        final LogbackValve logbackValve = new LogbackValve();
        logbackValve.setFilename( "logback-access.xml" );
        logbackValve.setQuiet( true );
        tomcatServletWebServerFactory.addContextValves( logbackValve );
        return tomcatServletWebServerFactory;
    }

}