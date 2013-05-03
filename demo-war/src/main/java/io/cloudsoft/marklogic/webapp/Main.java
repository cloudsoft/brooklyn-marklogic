package io.cloudsoft.marklogic.webapp;

import com.marklogic.client.DatabaseClient;
import com.marklogic.client.DatabaseClientFactory;
import com.marklogic.client.document.JSONDocumentManager;
import com.marklogic.client.io.InputStreamHandle;

import java.io.*;

public class Main {

    public static void main(String[] args){
        String host = "ec2-174-129-97-255.compute-1.amazonaws.com";
        int port = 8011;
        String user = "admin";
        String password = "hap00p";
        DatabaseClientFactory.Authentication authType= DatabaseClientFactory.Authentication.DIGEST;
        DatabaseClient client = DatabaseClientFactory.newClient(host, port, user, password, authType);
        JSONDocumentManager docMgr = client.newJSONDocumentManager();

        String json = "{\"product\":{\n" +
                "    \"name\":\"Flipper\",\n" +
                "    \"industry\":\"Real Estate\",\n" +
                "    \"description\":\"Discovers correlations and trending criteria. Finds neighborhoods matching specified criteria. Sends alert when a property qualifies for criteria.\"\n" +
                "}}\n";

        // convert String into InputStream
        InputStream is = new ByteArrayInputStream(json.getBytes());

        // create a handle on the content
        InputStreamHandle handle = new InputStreamHandle(is);

        // write the document content
        docMgr.write("/example/flipper.json", handle);

        System.out.println("Wrote /example/flipper.json content");

        client.release();
    }
}
