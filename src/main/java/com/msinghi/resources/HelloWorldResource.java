package com.msinghi.resources;

import java.io.IOException;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriInfo;

import org.apache.commons.codec.binary.Base64;

import com.eaap.auth.client.AsyncAuthenticationServiceClient;
import com.eaap.auth.client.EaapClient;
import com.eaap.auth.client.response.ValidationResponse;
import com.eaap.auth.client.response.ValidationResponse.ValidationStatus;
import com.msinghi.monitors.SandboxMonitors;

@Path("foo")
@Produces(MediaType.APPLICATION_JSON)
public class HelloWorldResource {

    private static String clientId = "981322e1-cd56-42fa-afe1-a9fd0391e7ea";
    private static String sharedSecret = "UwOtigLNkR5Zt1iTXME7aQ==";
    private static byte[] encodedKey = Base64.decodeBase64(sharedSecret);
    
    
    private static EaapClient eaapClient;
    
    static {
        try {
            eaapClient = EaapClient.Builder
   
                .newBuilder()
                .setCredentials(clientId, encodedKey)
                .enableCache(30)
                .setAuthServiceClient(
                        new AsyncAuthenticationServiceClient(
                                "https://BELC02KW1TJFFT3.sea.corp.expecn.com:8443/auth/clients", clientId, encodedKey))
                .build();
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
    
    @GET
    public Response healthcheck() throws IOException {

        SandboxMonitors.HELLO.increment();
        String hello = "Hello Bar";
        Response r = Response.ok(hello).build();

        return r;
    }

    @GET
    @Path("auth")
    public Response helloAuth(@Context UriInfo uriInfo, @Context HttpHeaders httpHeaders) throws Exception {

        SandboxMonitors.HELLO.increment();
        String authorizationHeader = httpHeaders.getHeaderString("Authorization");

        if (authorizationHeader == null) {
            Thread.sleep(10000);
        }
        
        long startTime = System.nanoTime();
        ValidationResponse response = eaapClient.validate(authorizationHeader,
                "BELC02KW1TJFFT3.sea.corp.expecn.com:8080", "/sandbox/foo/auth", "GET");
        long elapsedTime = System.nanoTime() - startTime;
        
        if (response.getValidationStatus().equals(ValidationStatus.VALID)) {
            SandboxMonitors.HELLO_ALLOWED.increment();
            String hello = "Hello Bar";
            return Response.ok(hello).build();
        } else {
            SandboxMonitors.HELLO_DENIED.increment();
            return Response.status(Status.UNAUTHORIZED).build();
        }

    }
}
