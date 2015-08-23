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
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

public class WSHelper {

    public static Document convertStringToDom(String domXMLSTring) throws Exception {
        DOMParser parser = new DOMParser();
        parser.parse(new InputSource(new java.io.StringReader(domXMLSTring)));
        return (parser.getDocument());
    }

    public static String convertDomToString(Document doc) throws Exception {
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
