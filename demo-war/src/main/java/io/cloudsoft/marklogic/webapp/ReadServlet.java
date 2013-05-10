package io.cloudsoft.marklogic.webapp;

import com.marklogic.client.DatabaseClient;
import com.marklogic.client.DatabaseClientFactory;
import com.marklogic.client.document.JSONDocumentManager;
import com.marklogic.client.io.StringHandle;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;

public class ReadServlet extends HttpServlet {

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
        if(documentId == null){
            throw new RuntimeException("Parameter 'id' is not found in request");
        }
        StringHandle handle = new StringHandle();
        docMgr.read(documentId, handle);
        PrintWriter out = response.getWriter();
        out.write(handle.get());
    }
}
