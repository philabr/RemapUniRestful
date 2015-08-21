package com.metaminer;

import com.sun.org.apache.xerces.internal.parsers.DOMParser;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

public class Main {
    private static String baseURL = "http://localhost:6427";

    public static void main(String[] args) {

        // Enterprise Authentication Credentials
        String boUsername = "Administrator";
        String boPassword = "ph!l!!";
        String boAuthType = "secEnterprise";

        // Enable Fiddler Trace.  This causes the requests to go through a proxy on port 8888 which fiddler listens on
        boolean enableFiddler = false;

        // Sample Variables
        String reportID = "55106";
        String sourceUniverseID = "54756"; // Only change dataproviders based off of this universe
        String destUniverseID = "54780";  // Change those dataproviders to use this Universe

        // Restful URL's
        final String baseURL = "http://localhost:6427/biprws";
        final String logonURL = baseURL + "/logon/long";
        final String logoffURL = baseURL + "/logoff";
        final String baseWebiURL = baseURL + "/raylight/v1/documents";

        try {
            String xmlString = "";
            String documentID = "";
            String logonToken = "";

// -----------------------
// Logon to Enterprise
// -----------------------

            // No logontoken detected - so create one
            System.out.println("No LogonToken found - Creating one </br>");
            xmlString = "<attrs><attr name=\"userName\" type=\"string\" >" + boUsername + "</attr><attr name=\"password\" type=\"string\" >" + boPassword + "</attr><attr name=\"auth\" type=\"string\" possibilities=\"secEnterprise,secLDAP,secWinAD,secSAPR3\">" + boAuthType + "</attr></attrs>";
            String logonXML = restPost(logonURL, xmlString, "", "", "", "");

            // The quotes are added because the webi URL require quotes around the token
            logonToken = "\"" + getLogonTokenFromXML(logonXML) + "\"";
            // Now that we have a logonToken - it must be included in the header for a future RestFul calls


// ---------------------------------------------------
// Retrieve the dataprovider collection for the report
// ---------------------------------------------------

            System.out.println("Looking at report " + reportID + " for a dataprovider based off of universe with ID " + sourceUniverseID + "</br>");
// First retrieve the webi report dataproviders so we can iterate through them
            xmlString = restGet(baseWebiURL + "/" + reportID + "/dataproviders", "X-SAP-LogonToken", logonToken, "", "");

// Now iterate through the result
            Document myDoc = convertStringToDom(xmlString);

// Now Loop through all the dataprovider nodes that were returned
// It is assumed that each dataprovider node will only have one ID tag
            NodeList dpNodes = myDoc.getElementsByTagName("id");

            for (int j = 0; j < dpNodes.getLength(); j++) {
                Element dpElement = (Element) dpNodes.item(j);

                // Get the string value
                String nodeID = (String) dpElement.getTextContent();

                System.out.println("Looking at DataProvider: " + nodeID + "</br>");
// ------------------------------------------------------
// Verify what universe that dataprovider is based off of
// ------------------------------------------------------

                // Now we need to check what universe this dataprovider is based off of.  To do that, we need to do a second query
                String xmlString2 = restGet(baseWebiURL + "/" + reportID + "/dataproviders/" + nodeID, "X-SAP-LogonToken", logonToken, "", "");

                // Now iterate through the result
                Document myDoc2 = convertStringToDom(xmlString2);

                // There should only be 1 result with the tag dataSourceID
                NodeList UnivNodes = myDoc2.getElementsByTagName("dataSourceId");
                Element univNode = (Element) UnivNodes.item(0);

                // Retrieve the value of the node
                String univValue = (String) univNode.getTextContent();

                System.out.println("Universe for " + nodeID + " is " + univValue + "</br>");
                // Now compare the universe value to verify if we should change this dataprovider
                if (sourceUniverseID.equals(univValue)) {
                    System.out.println("Source Universe to look for is: " + sourceUniverseID + " - match found - changing universe </br>");

                    // Confirmed - now change universe
// ------------------------------------------------------
// Get the default mappings for changing to a different universe
// ------------------------------------------------------

                    // First get the default mappings
                    String changeUnivUrl = baseWebiURL + "/" + reportID + "/dataproviders/mappings?originDataproviderIds=" + nodeID + "&targetDatasourceId=" + destUniverseID;
                    String xmlString3 = restGet(changeUnivUrl, "X-SAP-LogonToken", logonToken, "", "");
// -----------------
// Commit Changes
// -----------------

                    // Now do the actual change
                    String xmlString4 = restPost(changeUnivUrl, xmlString3, "X-SAP-LogonToken", logonToken, "", "");
                    System.out.println("Result of call: " + xmlString4 + "</br>");

                    // need to save the report after making this call

                    String putUrl = baseWebiURL + "/" + reportID;

                    String xmlString5 = restPut(putUrl,"", "X-SAP-LogonToken",logonToken, "","");

                }
            }

// Now log off
            xmlString = restPost(logoffURL, "", "X-SAP-LogonToken", logonToken, "", "");


            System.out.println("Logged off - done");

        } catch (IOException eIO) {
            System.out.println("IO Exception: " + eIO);
        } catch (Exception ex) {
            System.out.println("Exception: " + ex);
        }
    }

    public static Document convertStringToDom(String domXMLSTring) throws Exception {
        DOMParser parser = new DOMParser();
        parser.parse(new InputSource(new java.io.StringReader(domXMLSTring)));
        return (parser.getDocument());
    }

    public static String convertDomToString(Document doc) throws Exception {
// Now convert the document back to a string
        TransformerFactory tf = TransformerFactory.newInstance();
        Transformer transformer = tf.newTransformer();
        transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
        StringWriter writer = new StringWriter();
        transformer.transform(new DOMSource(doc), new StreamResult(writer));
        String output = writer.getBuffer().toString().replaceAll("\n|\r", "");
        return (output);
    }

    public static String buildEncodedURLString(String basePage, String url, String urlTitle) throws Exception {
        String returnURL = "";

        if (url.equals("")) {
            returnURL = urlTitle;
        } else {
            List<NameValuePair> qparams = new ArrayList<NameValuePair>();
            qparams.add(new BasicNameValuePair("queryURL", url));
            returnURL = "<a href=\"" + basePage + "?" + URLEncodedUtils.format(qparams, "UTF-8") + "\">" + urlTitle + "</a>";
        }
        return (returnURL);
    }

    public static String getLogonTokenFromXML(String xmlString) throws Exception {
        Document doc = convertStringToDom(xmlString);
        NodeList nodes = doc.getElementsByTagName("attr");
        for (int i = 0; i < nodes.getLength(); i++) {
            Element element = (Element) nodes.item(i);

            // Is this the correct XML token
            if (element.getAttribute("name").equals("logonToken")) {
                return (element.getTextContent());
            }
        }
        return ("");
    }

    public static String restGet(String urlStr, String param1Name, String param1Value, String
            param2Name, String param2Value) throws Exception {
        HttpClient httpclient = new DefaultHttpClient();

        try {

            HttpGet httpget = new HttpGet(urlStr);

            httpget.addHeader("Accept", "application/xml");
            httpget.addHeader("Content-Type", "application/xml");

            if (!param1Name.equals("")) {
                httpget.addHeader(param1Name, param1Value);
            }
            if (!param2Name.equals("")) {
                httpget.addHeader(param2Name, param2Value);
            }

            HttpResponse response = httpclient.execute(httpget);
            int statusCode = response.getStatusLine().getStatusCode();
            HttpEntity entity = response.getEntity();
            String responseBody = (String) EntityUtils.toString(entity);
            return (responseBody);
        } finally {
            // When HttpClient instance is no longer needed,
            // shut down the connection manager to ensure
            // immediate deallocation of all system resources
            httpclient.getConnectionManager().shutdown();
        }
    }

    // Allow for two parameters to be passed to the post request along with the XML string.
// The most common one will be the "X-SAP-LogonToken" parameter
    public static String restPost(String urlStr, String XMLString, String param1Name, String param1Value, String param2Name, String param2Value) throws Exception {

        HttpClient httpclient = new DefaultHttpClient();
        try {
            HttpPost httpPost = new HttpPost(urlStr);
            httpPost.addHeader("Accept", "application/xml");
            httpPost.addHeader("Content-Type", "application/xml");

            if (!param1Name.equals("")) {
                httpPost.addHeader(param1Name, param1Value);
            }
            if (!param2Name.equals("")) {
                httpPost.addHeader(param2Name, param2Value);
            }

            httpPost.setEntity(new StringEntity(XMLString));

            HttpResponse response = httpclient.execute(httpPost);
            int statusCode = response.getStatusLine().getStatusCode();
            HttpEntity entity = response.getEntity();
            String responseBody = (String) EntityUtils.toString(entity);
            return (responseBody);
        } finally {
            // When HttpClient instance is no longer needed,
            // shut down the connection manager to ensure
            // immediate deallocation of all system resources
            httpclient.getConnectionManager().shutdown();
        }
    }

    // Allow for two parameters to be passed to the post request along with the XML string.
// The most common one will be the "X-SAP-LogonToken" parameter
    public static String restPut( String urlStr, String XMLString, String param1Name, String param1Value, String param2Name, String param2Value) throws Exception {

        HttpClient httpclient = new DefaultHttpClient();
        try {


            HttpPut httpPut = new HttpPut(urlStr);
            httpPut.addHeader("Accept", "application/xml");
            httpPut.addHeader("Content-Type", "application/xml");

            if (!param1Name.equals("")) {
                httpPut.addHeader(param1Name, param1Value);
            }
            if (!param2Name.equals("")) {
                httpPut.addHeader(param2Name, param2Value);
            }

            httpPut.setEntity(new StringEntity(XMLString));

            HttpResponse response = httpclient.execute(httpPut);
            int statusCode = response.getStatusLine().getStatusCode();
            HttpEntity entity = response.getEntity();
            String responseBody = (String) EntityUtils.toString(entity);
            return (responseBody);
        } finally {
            // When HttpClient instance is no longer needed,
            // shut down the connection manager to ensure
            // immediate deallocation of all system resources
            httpclient.getConnectionManager().shutdown();
        }
    }

}