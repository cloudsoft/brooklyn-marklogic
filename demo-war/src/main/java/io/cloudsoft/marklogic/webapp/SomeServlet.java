package io.cloudsoft.marklogic.webapp;

import com.marklogic.client.DatabaseClient;
import com.marklogic.client.DatabaseClientFactory;
import com.marklogic.client.document.JSONDocumentManager;
import com.marklogic.client.io.InputStreamHandle;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;

public class SomeServlet extends HttpServlet {

    public void doGet(HttpServletRequest request,HttpServletResponse response) throws ServletException, IOException {
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

        // Use "request" to read incoming HTTP headers (e.g. cookies)
        // and HTML form data (e.g. data the user entered and submitted)

        // Use "response" to specify the HTTP response line and headers
        // (e.g. specifying the content type, setting cookies).

        PrintWriter out = response.getWriter();
        out.write("Json inserted");
        // Use "out" to send content to browser
    }
}
