package ru.spbau.mit;

import org.junit.Test;

import java.io.*;
import java.net.BindException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertEquals;

public class Tests {
    private static final int MAX_PORT = 20000;
    private static final int MIN_PORT = 10000;
    private static final int CNT_DIR = 4;

    @Test
    public void testGet() throws IOException {
        Path path = Files.createTempDirectory("FTP");
        File file = new File(path.toString() + File.separator + "tmp");

        PrintWriter writer = new PrintWriter(file);

        String fileString = "test   @";
        writer.print(fileString);

        writer.close();

        OutputStream output = new OutputStream() {
            private StringBuilder string = new StringBuilder();

            @Override
            public void write(int b) throws IOException {
                this.string.append((char) b);
            }

            public String toString() {
                return this.string.toString();
            }
        };

        int port = 0;
        Random rnd = new Random();
        port = rnd.nextInt(MAX_PORT - MIN_PORT) + MIN_PORT;

        Server server;
        while (true) {
            try {
                server = new Server(port);
                break;
            } catch (BindException e) {
                port = rnd.nextInt(MAX_PORT - MIN_PORT) + MIN_PORT;
            }
        }

        server.start();

        Client client = new Client("localhost", port);

        try {
            InputStream is = client.get(path.toString() + File.separator + "tmp");
            String val = "";
            int currentVal = is.read();
            while (currentVal != -1) {
                val += ((char) currentVal);
                currentVal = is.read();
            }

            assertEquals(val, fileString);
        } finally {
            server.stop();
            client.close();
        }
    }

    @Test
    public void testList() throws IOException {
        String[] fileName = new String[]{"dir1", "dir2", "dir3", "dir4", "file1", "file2", "file3", "file4"};

        Set<Client.FileEntry> setOfFiles = new HashSet<Client.FileEntry>();

        Path path = Files.createTempDirectory("FTP");
        for (int i = 0; i < CNT_DIR; ++i) {
            (new File(path.toString() + File.separator + fileName[i])).mkdir();
            setOfFiles.add(new Client.FileEntry(fileName[i], true));
        }
        for (int i = CNT_DIR; i < fileName.length; ++i) {
            (new File(path.toString() + File.separator + fileName[i])).createNewFile();
            setOfFiles.add(new Client.FileEntry(fileName[i], false));
        }

        int port = 0;
        Random rnd = new Random();
        port = rnd.nextInt(MAX_PORT - MIN_PORT) + MIN_PORT;

        Server server;
        while (true) {
            try {
                server = new Server(port);
                break;
            } catch (BindException e) {
                port = rnd.nextInt(MAX_PORT - MIN_PORT) + MIN_PORT;
            }
        }

        server.start();

        Client client = new Client("localhost", port);

        try {
            ArrayList<Client.FileEntry> ls = client.list(path.toString());
            assertEquals(ls.size(), fileName.length);

            Set<Client.FileEntry> lsSet = new HashSet<>();
            lsSet.addAll(ls);

            assertTrue(lsSet.containsAll(setOfFiles));
            new File(path.toString()).delete();
        } finally {
            server.stop();
            client.close();
        }
    }
}
