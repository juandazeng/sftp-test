package com.redhat.jz;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;

@Path("/sftp")
public class MainResource {

    @GET
    // Try this:
    //    <domain:port/context>/sftp?file=/pub/example/readme.txt
    @Produces(MediaType.TEXT_PLAIN)
    public String sftp(@QueryParam("file") @DefaultValue("/pub/example/readme.txt") String targetFileFullPath) {
        final String KNOWN_HOSTS = "|1|x8FhSjRJCz+uzGIZ0d8DRAZL2Zo=|tGCk5/oZLqV2HCpN90LBPrq7W2c= ssh-rsa AAAAB3NzaC1yc2EAAAABJQAAAQEAkRM6RxDdi3uAGogR3nsQMpmt43X4WnwgMzs8VkwUCqikewxqk4U7EyUSOUeT3CoUNOtywrkNbH83e6/yQgzc3M8i/eDzYtXaNGcKyLfy3Ci6XOwiLLOx1z2AGvvTXln1RXtve+Tn1RTr1BhXVh2cUYbiuVtTWqbEgErT20n4GWD4wv7FhkDbLXNi8DX07F9v7+jH67i0kyGm+E3rE+SaCMRo3zXE6VO+ijcm9HdVxfltQwOYLfuPXM2t5aUSfa96KJcA0I4RCMzA/8Dl9hXGfbWdbD2hK1ZQ1pLvvpNPPyKKjPZcMpOznprbg+jIlsZMWIHt7mq2OJXSdruhRrGzZw==";
        final String SFTP_HOST_NAME = "test.rebex.net";
        final int SFTP_HOST_PORT = 22;
        final String SFTP_USER_NAME = "demo";
        final String SFTP_PASSWORD = "password";
        final String SFTP_CHANNEL = "sftp";

        String result = "Empty";
        JSch jsch = new JSch();

        try {
            // // DEBUG ONLY: no host key checking
            // java.util.Properties config = new java.util.Properties(); 
            // config.put("StrictHostKeyChecking", "no");
            // session.setConfig(config);
            //
            // The secure approach is to create/add an entry to a known_hosts file with the following command
            //    $ ssh-keyscan -H -t rsa example.org >> known_hosts
            // example.org is the target SFTP host.
            // The reason why this command is necessary is because SSH server sends ECDSA fingerprint,
            // while JSch prefers SHA_RSA.
            
            // In this example, we're using a hardcoded string to represent the above known_hosts entry.
            // This can be stored somewhere else more appropriate, e.g. in a ConfigMap, etc.
            try (InputStream knownHostsInputStream = new ByteArrayInputStream(KNOWN_HOSTS.getBytes())) {
                jsch.setKnownHosts(knownHostsInputStream);
            }
            
            // If key authentication is required, add the following line pointing to the private key
            // jsch.addIdentity("/home/currentuser/.ssh/id_rsa");

            Session session = jsch.getSession(SFTP_USER_NAME, SFTP_HOST_NAME, SFTP_HOST_PORT);
            session.setPassword(SFTP_PASSWORD);
            session.connect();

            Channel channel = session.openChannel(SFTP_CHANNEL);
            channel.connect();
            ChannelSftp sftpChannel = (ChannelSftp) channel;

            // Try to download/read a file from the SFTP server.
            // To upload, use the sftpChannel.put(...) method
            try (InputStream inputStream = sftpChannel.get(targetFileFullPath)) {
                result = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
            }

            sftpChannel.exit();
            session.disconnect();

        } catch (Exception ex) {
            ex.printStackTrace();
        }

        // Return the content of the target file 
        return targetFileFullPath + ":\n\n" + result;
    }
}