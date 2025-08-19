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
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import io.installr.lib.deployer.Deployment;
import io.installr.lib.deployer.DeploymentException;
import io.installr.lib.deployer.DeploymentResponse;
import io.installr.lib.deployer.DeploymentStatus;


@RestController
public class KeycloakDeployerController {

    /**
     * Spring does not map form parameters of multipart/form-data requests to JSON nor does it support to set
     * <code>@RequestParam</code> within a DTO. So we need to have all parameters in the endpoint spec rather than in a
     * custom DTO.
     */

    @Autowired
    KeycloakDeployer keycloakDeployer;

    @PutMapping( path = "/Deployer.deploy/{name}", consumes = "multipart/form-data", produces = "application/json" )
    public ResponseEntity<DeploymentResponse> deploy(
        @PathVariable( value = "name"      , required = true  ) String name
    ,   @RequestPart ( value = "data"      , required = true  ) MultipartFile data
    ,   @RequestPart ( value = "media-type", required = true  ) String mediaType
    ,   @RequestPart ( value = "env"       , required = true  ) Map<String, String> env
    ,   @RequestPart ( value = "encoding"  , required = false ) String encoding
    ) {
        return handleRequest( () -> keycloakDeployer.deploy( toDeployment( name, data, mediaType, env, encoding ) ) );
    }

    @DeleteMapping( path = "/Deployer.undeploy/{name}", consumes = "multipart/form-data", produces = "application/json" )
    public ResponseEntity<DeploymentResponse> undeploy(
        @PathVariable( value = "name"      , required = true  ) String name
    ,   @RequestPart ( value = "data"      , required = true  ) MultipartFile data
    ,   @RequestPart ( value = "media-type", required = true  ) String mediaType
    ,   @RequestPart ( value = "env"       , required = true  ) Map<String, String> env
    ,   @RequestPart ( value = "encoding"  , required = false ) String encoding
    ) {
        return handleRequest( () -> keycloakDeployer.undeploy( toDeployment( name, data, mediaType, env, encoding ) ) );
    }

    private ResponseEntity<DeploymentResponse> handleRequest( Runnable runnable ) {
        try {
            runnable.run();
            return ResponseEntity.ok().body( DeploymentResponse.builder().status( DeploymentStatus.Success ).build() );
        }
        catch ( DeploymentException ex ) {
            return ResponseEntity.badRequest().body(
                DeploymentResponse.builder().status( DeploymentStatus.Failed ).message( ex.getMessage() ).build()
            );
        }
        catch ( Throwable t ) {
            return ResponseEntity.internalServerError().body(
                DeploymentResponse.builder().status( DeploymentStatus.Failed ).message( t.getMessage() ).build()
            );
        }
    }

    private Deployment toDeployment(
        String name, MultipartFile data, String type, Map<String, String> env, String encoding ) {
        Deployment deployment = new Deployment();
        deployment.setName( name );
        deployment.setData( getBytes( data ) );
        deployment.setMediaType( type );
        deployment.setEncoding( encoding );
        deployment.setEnv( env );
        return deployment;
    }

    private byte[] getBytes( MultipartFile data ) {
        try {
            return data.getBytes();
        }
        catch ( IOException ex ) {
            throw new DeploymentException( "Error reading data: %s", ex.getMessage() );
        }
    }

}
