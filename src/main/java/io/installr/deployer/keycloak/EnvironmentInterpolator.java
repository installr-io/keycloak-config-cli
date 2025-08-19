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

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class EnvironmentInterpolator {

    private static final Logger logger = LoggerFactory.getLogger( EnvironmentInterpolator.class );

    public String interpolate( String json, Map<String, String> env ) {
        final Pattern pattern = Pattern.compile( "\\$\\{(.*?)(:-(.*?))?\\}" );
        final Matcher matcher = pattern.matcher( json );
        final StringBuilder resultBuilder = new StringBuilder();
        while( matcher.find() ) {
            String propertyKey = matcher.group( 1 );
            String propertyDef = matcher.group( 3 );
            logger.debug( "found ref-key: {}", propertyKey );
            String propertyVal = env.get( propertyKey );
            logger.debug( "        found: {}", propertyVal );
            logger.debug( "      default: {}", propertyDef );
            if ( propertyVal == null || propertyVal.isBlank() ) {
                logger.warn( "invalid property reference: {}", propertyKey );
                propertyVal = propertyDef == null ? "" : propertyDef;
            }
            logger.debug( "        value: {}", propertyVal );
            matcher.appendReplacement( resultBuilder, propertyVal );
        }
        matcher.appendTail( resultBuilder );
        return trimArrays( resultBuilder.toString() );
    }
    
    private String trimArrays( final String json ) {
        final Pattern pattern = Pattern.compile( "(?<=\\[)\\s*\".*?(?=\\])", Pattern.DOTALL );
        final Matcher matcher = pattern.matcher( json );
        final StringBuilder resultBuilder = new StringBuilder();
        while( matcher.find() ) {
            matcher.appendReplacement( resultBuilder, toArrayString( matcher.group() ) );
        }
        matcher.appendTail( resultBuilder );
        return resultBuilder.toString();
    }
    
    private String toArrayString( final String str ) {
        return Stream.of( str.split( "," ) )
                // remove quotes to filter out blanks
                .map( token -> token.toString().trim().replace( "\"", "" ) )
                .filter( token -> !token.isBlank() )
                // add quotes
                .map( token -> "\"" + token + "\"" )
                .collect( Collectors.joining( "," ) )
        ;
    }
    
}
