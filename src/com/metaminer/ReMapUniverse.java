package com.metaminer;


import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

public class ReMapUniverse {

    public static void remap(String wsServerPort, String boUsername, String boPassword, String reportID, String sourceUniverseID, String destUniverseID) throws Exception {
        final String boAuthType = "secEnterprise";

        final String baseURL = "http://"+wsServerPort+"/biprws";
        final String logonURL = baseURL + "/logon/long";
        final String logoffURL = baseURL + "/logoff";
        final String baseWebiURL = baseURL + "/raylight/v1/documents";


            String logonToken = doLogin(boUsername, boPassword, boAuthType, logonURL);

            NodeList dpNodes = getDataProviderListForReport(reportID, sourceUniverseID, baseWebiURL, logonToken);

            for (int j = 0; j < dpNodes.getLength(); j++) {
                Element dpElement = (Element) dpNodes.item(j);

                String nodeID = dpElement.getTextContent();

                System.out.println("Looking at DataProvider: " + nodeID + "");

                String univValue = getUniverseForDataProvider(reportID, baseWebiURL, logonToken, nodeID);


                // Now compare the universe value to verify if we should change this dataprovider
                if (sourceUniverseID.equals(univValue)) {
                    System.out.println("Source Universe is: " + sourceUniverseID + " - match found - changing universe </br>");

                    doChangeUsingDefaultMapping(reportID, destUniverseID, baseWebiURL, logonToken, nodeID);
                    commitChange(reportID, baseWebiURL, logonToken);
                }
            }

            doLogoff(logoffURL, logonToken);


    }

    private static void doLogoff(String logoffURL, String logonToken) throws Exception {
        String xmlString = WSHelper.restPost(logoffURL, "", "X-SAP-LogonToken", logonToken, "", "");


        System.out.println("Logged off - done");
    }

    private static void commitChange(String reportID, String baseWebiURL, String logonToken) throws Exception {
        String putUrl = baseWebiURL + "/" + reportID;

        String xmlString5 = WSHelper.restPut(putUrl, "", "X-SAP-LogonToken", logonToken, "", "");
    }

    private static void doChangeUsingDefaultMapping(String reportID, String destUniverseID, String baseWebiURL, String logonToken, String nodeID) throws Exception {
        // First get the default mappings
        String changeUnivUrl = baseWebiURL + "/" + reportID + "/dataproviders/mappings?originDataproviderIds=" + nodeID + "&targetDatasourceId=" + destUniverseID;
        String xmlString3 = WSHelper.restGet(changeUnivUrl, "X-SAP-LogonToken", logonToken, "", "");
        System.out.println("Default mapping: " + xmlString3 + "");

        // Now do the actual change
        String xmlString4 = WSHelper.restPost(changeUnivUrl, xmlString3, "X-SAP-LogonToken", logonToken, "", "");
        System.out.println("Result of call: " + xmlString4 + "");
    }

    private static String getUniverseForDataProvider(String reportID, String baseWebiURL, String logonToken, String nodeID) throws Exception {
        // Now we need to check what universe this dataprovider is based off of.  To do that, we need to do a second query
        String xmlString2 = WSHelper.restGet(baseWebiURL + "/" + reportID + "/dataproviders/" + nodeID, "X-SAP-LogonToken", logonToken, "", "");

        Document myDoc2 = WSHelper.convertStringToDom(xmlString2);

        // There should only be 1 result with the tag dataSourceID
        NodeList UnivNodes = myDoc2.getElementsByTagName("dataSourceId");
        Element univNode = (Element) UnivNodes.item(0);

        // Retrieve the value of the node
        String univValue = univNode.getTextContent();

        System.out.println("Universe for " + nodeID + " is " + univValue + "");
        return univValue;
    }

    private static NodeList getDataProviderListForReport(String reportID, String sourceUniverseID, String baseWebiURL, String logonToken) throws Exception {
        String xmlString;
        System.out.println("Looking at report " + reportID + " for a dataprovider based off of universe with ID " + sourceUniverseID + "");

        xmlString = WSHelper.restGet(baseWebiURL + "/" + reportID + "/dataproviders", "X-SAP-LogonToken", logonToken, "", "");

        Document myDoc = WSHelper.convertStringToDom(xmlString);

        return myDoc.getElementsByTagName("id");
    }

    private static String doLogin(String boUsername, String boPassword, String boAuthType, String logonURL) throws Exception {
        String xmlString;
        String logonToken;
        System.out.println("Creating login token...");
        xmlString = "<attrs><attr name=\"userName\" type=\"string\" >" + boUsername + "</attr><attr name=\"password\" type=\"string\" >" + boPassword + "</attr><attr name=\"auth\" type=\"string\" possibilities=\"secEnterprise,secLDAP,secWinAD,secSAPR3\">" + boAuthType + "</attr></attrs>";
        String logonXML = WSHelper.restPost(logonURL, xmlString, "", "", "", "");

        // The quotes are added because the webi URL require quotes around the token
        logonToken = "\"" + WSHelper.getLogonTokenFromXML(logonXML) + "\"";
        return logonToken;
    }
}
