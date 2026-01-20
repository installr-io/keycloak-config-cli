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

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.apache.commons.codec.digest.DigestUtils;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.ClientScopeRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;

import de.adorsys.keycloak.config.model.RealmImport;
import de.adorsys.keycloak.config.properties.KeycloakConfigProperties;
import de.adorsys.keycloak.config.provider.KeycloakProvider;
import de.adorsys.keycloak.config.service.ClientImportService;
import de.adorsys.keycloak.config.service.RealmImportService;
import de.adorsys.keycloak.config.service.UserImportService;
import io.installr.lib.deployer.Deployer;
import io.installr.lib.deployer.Deployment;
import io.installr.lib.deployer.DeploymentException;
import jakarta.annotation.PostConstruct;
import jakarta.ws.rs.ClientErrorException;
import jakarta.ws.rs.core.Response;

@Service
public class KeycloakDeployer implements Deployer {

    private static final Logger logger = LoggerFactory.getLogger( KeycloakDeployer.class );
    private static final String REALM_ATTR = "realm";

    private static enum MediaType {
        Realm ( "application/vnd.io.installr.kc-realm"  )
    ,   Role  ( "application/vnd.io.installr.kc-role"   )
    ,   Client( "application/vnd.io.installr.kc-client" )
    ,   Scope ( "application/vnd.io.installr.kc-scope"  )
    ,   User  ( "application/vnd.io.installr.kc-user"   )
    ;

        final String mediaTypeString;
        MediaType( String mediaTypeString ) { this.mediaTypeString = mediaTypeString; }

        static MediaType valueByMediaTypeString( String mediaTypeString ) {
            return Stream.of( values() )
                .filter( x -> x.mediaTypeString.equals( mediaTypeString ) )
                .findFirst()
                .orElseThrow(() -> new DeploymentException( "MediaType '%s' not supported.", mediaTypeString ) )
            ;
        }
    }

    @Autowired
    KeycloakConfigProperties configProperties;

    @Autowired
    EnvironmentInterpolator interpolator;

    @Autowired
    KeycloakProvider keycloakProvider;

    @Autowired
    RealmImportService realmImportService;

    @Autowired
    ClientImportService clientImportService;

    @Autowired
    UserImportService userImportService;

    @Autowired
    ObjectMapper objectMapper;

    @PostConstruct
    void init() {
        objectMapper.configure( JsonParser.Feature.ALLOW_COMMENTS, true );
    }

    @Override
    public void deploy( Deployment deployment ) throws DeploymentException {
        doWork( deployment, (mediaType, json) -> {
            switch ( mediaType ) {
                case Realm  : deployRealm ( json, deployment.getEnv() ); break;
                case Role   : deployRole  ( json, deployment.getEnv() ); break;
                case Client : deployClient( json, deployment.getEnv() ); break;
                case Scope  : deployScope ( json, deployment.getEnv() ); break;
                case User   : deployUser  ( json, deployment.getEnv() ); break;
                default:
                    throw new DeploymentException( "Unsupported media type: %s", mediaType );
            }
        });
    }

    @Override
    public void undeploy( Deployment deployment ) throws DeploymentException {
        doWork( deployment, (mediaType, json) -> {
            switch ( mediaType ) {
                case Realm  : undeployRealm ( json, deployment.getEnv() ); break;
                case Role   : undeployRole  ( json, deployment.getEnv() ); break;
                case Client : undeployClient( json, deployment.getEnv() ); break;
                case Scope  : undeployScope ( json, deployment.getEnv() ); break;
                case User   : undeployUser  ( json, deployment.getEnv() ); break;
                default:
                    throw new DeploymentException( "Unsupported media type: %s", mediaType );
            }
        });
    }

    private void doWork( Deployment deployment, BiConsumer<MediaType, String> job ) {
        configProperties.setUrl( deployment.getEnv().get( "keycloak.url" ) );
        configProperties.setUser( deployment.getEnv().get( "keycloak.user" ) );
        configProperties.setPassword( deployment.getEnv().get( "keycloak.password" ) );
        try {
            final MediaType mediaType = MediaType.valueByMediaTypeString( deployment.getMediaType() );
            final String json = interpolator.interpolate(
                new String( deployment.getData(), toCharset( deployment.getEncoding() ) ), deployment.getEnv()
            );
            job.accept( mediaType, json );
        }
        catch ( ClientErrorException | IOException ex ) {
            logger.warn( "{} (ex={}): {}", deployment.getName(), ex.getClass().getName(), ex.getMessage() );
            throw new DeploymentException( ex.getMessage() );
        }
        catch ( Throwable t ) {
            logger.error( "STACKTRACE", t );
            throw t;
        }
        finally {
            keycloakProvider.getInstance().close();
        }
    }

    private Charset toCharset( String encoding ) {
        if ( encoding == null ) {
            return StandardCharsets.UTF_8;
        }
        return Charset.forName( encoding );
    }

    private void deployRealm( String json, Map<String, String> env ) throws IOException {
        RealmImport rr = objectMapper.readValue( json, RealmImport.class );
        rr.setChecksum( DigestUtils.sha256Hex( json ) );
        realmImportService.doImport( rr );
    }

    private void undeployRealm( String json, Map<String, String> env ) throws IOException {
        RealmRepresentation rr = objectMapper.readValue( json, RealmRepresentation.class );
        Keycloak kc = keycloakProvider.getInstance();
        boolean exists = kc.realms().findAll().stream()
            .anyMatch( r -> rr.getRealm().equalsIgnoreCase( r.getRealm() ) )
        ;
        if ( exists ) {
            kc.realm( rr.getRealm() ).remove();
        }
    }

    private void deployRole( String json, Map<String, String> env ) throws IOException {
        String realm = extractRealm( json, env.get( REALM_ATTR ) );
        logger.info( "Deploying role to realm '{}'", realm );
        RoleRepresentation rr = objectMapper.readValue( json, RoleRepresentation.class );
        Keycloak kc = keycloakProvider.getInstance();
        boolean exists = kc.realm( realm ).roles().list().stream()
            .anyMatch( r -> rr.getName().equalsIgnoreCase( r.getName() ) )
        ;
        if ( exists ) {
            kc.realm( realm ).roles().get( rr.getName() ).update( rr );
        }
        else {
            kc.realm( realm ).roles().create( rr );
        }
    }

    private void undeployRole( String json, Map<String, String> env ) throws IOException {
        String realm = extractRealm( json, env.get( REALM_ATTR ) );
        logger.info( "Un-deploying role from realm '{}'", realm );
        RoleRepresentation rr = objectMapper.readValue( json, RoleRepresentation.class );
        Keycloak kc = keycloakProvider.getInstance();
        boolean exists = kc.realm( realm ).roles().list().stream()
            .anyMatch( r -> rr.getName().equalsIgnoreCase( r.getName() ) )
        ;
        if ( exists ) {
            kc.realm( realm ).roles().get( rr.getName() ).remove();
        }
    }
    
    private void deployClient( String json, Map<String, String> env ) throws IOException {
        String realm = extractRealm( json, env.get( REALM_ATTR ) );
        logger.info( "Deploying client to realm '{}'", realm );
        Keycloak kc = keycloakProvider.getInstance();
        ClientRepresentation cr = objectMapper.readValue( json, ClientRepresentation.class );
        autoCreateNonExistentScopes( realm, cr.getDefaultClientScopes() );
        Optional<ClientRepresentation> cl = 
            kc.realm( realm ).clients().findByClientId( cr.getClientId() ).stream().findFirst()
        ;
        final String secret = env.get( cr.getClientId() + "_secret" );
        if ( cl.isPresent() ) {
            // Since the api does not update all attributes we remove the client first and re-create it with the same
            // id and secret.
            cr.setId( cl.get().getId() );
            if ( cr.getSecret() == null && secret == null ) {
                // If there is no secret set, we take the old secret
                ClientResource client = kc.realm( realm ).clients().get( cl.get().getId() );
                cr.setSecret( client.getSecret().getValue() );
            }
        }
        if ( cr.getSecret() == null && !cr.isPublicClient() ) {
            cr.setSecret( secret );
        }
        RealmRepresentation rr = new RealmRepresentation();
        rr.setRealm( realm );
        rr.setClients( List.of( cr ) );
        
        final String clientJson = objectMapper.writeValueAsString( rr );
        RealmImport ri = objectMapper.readValue( clientJson, RealmImport.class );
        ri.setChecksum( DigestUtils.sha256Hex( clientJson ) );
        clientImportService.doImport( ri );
    }

    private void undeployClient( String json, Map<String, String> env ) throws IOException {
        String realm = extractRealm( json, env.get( REALM_ATTR ) );
        logger.info( "Un-deploying client from realm '{}'", realm );
        ClientRepresentation cr = objectMapper.readValue( json, ClientRepresentation.class );
        Keycloak kc = keycloakProvider.getInstance();
        Optional<ClientRepresentation> cl = 
            kc.realm( realm ).clients().findByClientId( cr.getClientId() ).stream().findFirst()
        ;
        if ( cl.isPresent() ) {
            kc.realm( realm ).clients().get( cl.get().getId() ).remove();
        }
    }

    private void deployScope( String json, Map<String, String> env ) throws IOException {
        String realm = extractRealm( json, env.get( REALM_ATTR ) );
        logger.info( "Deploying scope to realm '{}'", realm );
        createScope( realm, objectMapper.readValue( json, ClientScopeRepresentation.class ) );
    }

    private void undeployScope( String json, Map<String, String> env ) throws IOException {
        String realm = extractRealm( json, env.get( REALM_ATTR ) );
        logger.info( "Un-deploying scope from realm '{}'", realm );
        ClientScopeRepresentation csr = objectMapper.readValue( json, ClientScopeRepresentation.class );
        Keycloak kc = keycloakProvider.getInstance();
        Optional<ClientScopeRepresentation> ocsr = kc.realm( realm ).clientScopes().findAll().stream()
            .filter( s -> csr.getName().equalsIgnoreCase( s.getName() ) )
            .findFirst()
        ;
        if ( ocsr.isPresent() ) {
            kc.realm( realm ).clientScopes().get( ocsr.get().getId() ).remove();
        }
    }
    
    private void deployUser( String json, Map<String, String> env ) throws IOException {
        final String realm = extractRealm( json, env.get( REALM_ATTR ) );
        logger.info( "Deploying user to realm '{}'", realm );
        
        final UserRepresentation ur = objectMapper.readValue( json, UserRepresentation.class );;

        final RealmRepresentation rr = new RealmRepresentation();
        rr.setRealm( realm );
        rr.setUsers( List.of( ur ) );
        
        final String clientJson = objectMapper.writeValueAsString( rr );
        RealmImport ri = objectMapper.readValue( clientJson, RealmImport.class );
        ri.setChecksum( DigestUtils.sha256Hex( clientJson ) );
        userImportService.doImport( ri );
    }

    private void undeployUser( String json, Map<String, String> env ) throws IOException {
        final String realm = extractRealm( json, env.get( REALM_ATTR ) );
        logger.info( "Un-deploying user from realm '{}'", realm );
        final Keycloak kc = keycloakProvider.getInstance();
        UserRepresentation ur = objectMapper.readValue( json, UserRepresentation.class );
        if ( ur.getId() == null ) {
            final List<UserRepresentation> search = kc.realm( realm ).users().search( ur.getUsername(), Boolean.TRUE );
            if ( search.isEmpty() ) {
                throw new DeploymentException( "User with username '%s' not found.", ur.getUsername() );
            }
            if ( search.size() > 1 ) {
                throw new DeploymentException( "Duplicate users found: %s.", ur.getUsername() );
            }
            ur = search.getFirst();
        }
        kc.realm( realm ).users().get( ur.getId() ).remove();
    }

    private String extractRealm( String json, String defaultRealm ) {
        final Pattern pattern = Pattern.compile( "//\s*@realm=(.*)" );
        final String firstLine = json.split( "\r?\n" )[0];
        final Matcher matcher = pattern.matcher( firstLine );
        if ( !matcher.matches() ) {
            return defaultRealm;
        }
        return matcher.group( 1 );
    }

    private Optional<ClientScopeRepresentation> findScope( String realm, String scopeName ) {
        Keycloak kc = keycloakProvider.getInstance();
        return kc.realm( realm ).clientScopes().findAll().stream()
            .filter( s -> scopeName.equalsIgnoreCase( s.getName() ) )
            .findFirst()
        ;
    }
    
    private void createScope( String realm, ClientScopeRepresentation csr ) {
        Optional<ClientScopeRepresentation> ocsr = findScope( realm, csr.getName() );
        Keycloak kc = keycloakProvider.getInstance();
        if ( ocsr.isPresent() ) {
            kc.realm( realm ).clientScopes().get( ocsr.get().getId() ).update( csr );
        }
        else {
            checkResponse( kc.realm( realm ).clientScopes().create( csr ) );
        }
    }

    private void autoCreateNonExistentScopes( String realm, List<String> scopes ) {
        if ( scopes == null || scopes.isEmpty() ) {
            return;
        }
        scopes.forEach(
            name -> {
                if ( findScope( realm, name ).isEmpty() ) {
                    logger.debug( "scope '{}' not found; auto-creating it...", name );
                    ClientScopeRepresentation csr = new ClientScopeRepresentation();
                    csr.setName( name );
                    csr.setDescription( "auto-generated" );
                    csr.setProtocol( "openid-connect" );
                    createScope( realm, csr );
                }
            }
        );
    }

    private void checkResponse( Response response ) {
        if ( response == null ) {
            return;
        }
        final Response.StatusType statusInfo = response.getStatusInfo();
        if ( statusInfo.getFamily() != Response.Status.Family.SUCCESSFUL ) {
            throw new IllegalStateException( statusInfo.getStatusCode() + " " + statusInfo.getReasonPhrase() );
        }
    }

}
