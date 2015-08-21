package com.metaminer;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

public class Main {
    public static void main(String[] args) {
        try {

            String baseURL = "http://52.17.237.152:6405/";

            URL url = new URL(
                    baseURL+"/biprws/raylight/v1/documents/7485/dataproviders/DP0/flows/0");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Accept", "text/plain");//change to text/plain if you want in CSV format
            String logonToken = "\"" + getLogonToken() + "\"";
            conn.setRequestProperty("X-SAP-LogonToken", logonToken);
            conn.setDoOutput(true);
            conn.setDoInput(true);
            conn.connect();
            if (conn.getResponseCode() != 200) {
                throw new RuntimeException("Failed : HTTP error code : "
                        + conn.getResponseCode());
            }
            BufferedReader br = new BufferedReader(new InputStreamReader(
                    (conn.getInputStream())));
            String output;
            // Exported CSV file will be generated at "C://rest.csv", location could be changed as required.
            File f1 = new File("C://rest.csv");
            f1.createNewFile();
            FileWriter fw = new FileWriter(f1);//change extension to .csv for csv format
            BufferedWriter bw = new BufferedWriter(fw);
            while ((output = br.readLine()) != null) {
                bw.write(output);
                bw.write("\n");
            }

            bw.close();
            conn.disconnect();

        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (ProtocolException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ParseException e) {

            e.printStackTrace();
        }

    }

    public static String getLogonToken() throws ParseException, IOException {

        String logontoken = null;

        URL url = new URL("http://localhost:6405/biprws/logon/long/");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Accept", "application/json");
        conn.setRequestProperty("Content-Type",
                "application/xml; charset=utf-8");
        conn.setDoInput(true);
        conn.setDoOutput(true);
        String body = "<attrs xmlns=\"http://www.sap.com/rws/bip\">"
                + "<attr name=\"userName\" type=\"string\">Administrator</attr>"
                + "<attr name=\"password\" type=\"string\">ph!l!!</attr>"
                + "<attr name=\"auth\" type=\"string\" possibilities=\"secEnterprise,secLDAP,secWinAD\">secEnterprise</attr>"
                + "</attrs>";

        int len = body.length();
        conn.setRequestProperty("Content-Length", Integer.toString(len));
        conn.connect();

        OutputStreamWriter out = new OutputStreamWriter(conn.getOutputStream());
        out.write(body, 0, len);
        out.flush();
        if (conn.getResponseCode() != 200) {
            throw new RuntimeException("Failed : HTTP error code : "
                    + conn.getResponseCode());
        }

        BufferedReader br = new BufferedReader(new InputStreamReader(
                (conn.getInputStream())));
        String jsontxt = br.readLine();
        JSONParser parser = new JSONParser();
        JSONObject json = (JSONObject) parser.parse(jsontxt);
        logontoken = (String) json.get("logonToken");
        conn.disconnect();

        return logontoken;
    }

}