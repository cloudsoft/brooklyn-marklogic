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

public class InsertServlet extends HttpServlet {

    public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String host = System.getProperty("marklogic.host");
        if(host == null)throw new IllegalStateException("marklogic.host system property is not set");

        final String portStr = System.getProperty("marklogic.port");
        if(portStr == null)throw new IllegalStateException("marklogic.port system property is not set");
        int port = Integer.parseInt(portStr);

        String user = System.getProperty("marklogic.user");
        if(user == null)throw new IllegalStateException("marklogic.user system property is not set");

        String password = System.getProperty("marklogic.password");
        if(password == null)throw new IllegalStateException("marklogic.password system property is not set");


        DatabaseClientFactory.Authentication authType = DatabaseClientFactory.Authentication.DIGEST;
        DatabaseClient client = DatabaseClientFactory.newClient(host, port, user, password, authType);

        JSONDocumentManager docMgr = client.newJSONDocumentManager();

        String documentId = request.getParameter("id");
        if (documentId == null) {
            throw new RuntimeException("parameter 'id' not found in request");
        }

        String document = request.getParameter("document");
        if (document == null) {
            throw new RuntimeException("parameter 'document' not found in request");
        }

        InputStream is = new ByteArrayInputStream(document.getBytes());

        InputStreamHandle handle = new InputStreamHandle(is);

        docMgr.write(documentId, handle);

        System.out.println("Wrote /example/flipper.json content");

        client.release();
        PrintWriter out = response.getWriter();
        out.write("Json inserted");
    }
}
