package com.redhat.jz;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;

@Path("/sftp")
public class MainResource {

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String sftp() {
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
            
            // In this example, we're using such a file called known_hosts.txt
            jsch.setKnownHosts(getClass().getClassLoader().getResource("known_hosts.txt").getPath());
            // If key authentication is required, add the following line pointing to the private key
            // jsch.addIdentity("/home/currentuser/.ssh/id_rsa");

            Session session = jsch.getSession( "demo", "test.rebex.net" );
            session.setPassword("password");

            session.connect();

            Channel channel = session.openChannel( "sftp" );
            channel.connect();

            ChannelSftp sftpChannel = (ChannelSftp) channel;

            // Try to download/read a file from the SFTP server.
            // To upload, use the sftpChannel.put(...) method
            try (InputStream inputStream = sftpChannel.get("/pub/example/readme.txt")) {
                result = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
            }

            sftpChannel.exit();
            session.disconnect();

        } catch (Exception ex) {
            ex.printStackTrace();
        }

        return result;
    }
}