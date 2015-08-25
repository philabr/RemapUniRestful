package com.metaminer;


import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

public class ReMapUniverse {

    public static void remap(String wsServerPort, String boUsername, String boPassword, String reportID, String sourceUniverseID, String destUniverseID) throws Exception {
        final String boAuthType = "secEnterprise";

        final String baseURL = "http://" + wsServerPort + "/biprws";
        final String logonURL = baseURL + "/logon/long";
        final String logoffURL = baseURL + "/logoff";
        final String baseWebiURL = baseURL + "/raylight/v1/documents";
        final String baseUnvURL = baseURL + "/raylight/v1/universes";


        String logonToken = doLogin(boUsername, boPassword, boAuthType, logonURL);

        NodeList dpNodes = getDataProviderListForReport(reportID, sourceUniverseID, baseWebiURL, logonToken);

        for (int j = 0; j < dpNodes.getLength(); j++) {
            Element dpElement = (Element) dpNodes.item(j);

            String nodeID = dpElement.getTextContent();

            System.out.println("Looking at DataProvider: " + nodeID + "");

            String univValue = getUniverseForDataProvider(reportID, baseWebiURL, logonToken, nodeID);


            // Now compare the universe value to verify if we should change this dataprovider
            if (sourceUniverseID.equals(univValue) || sourceUniverseID.equals("*")) {
                System.out.println("Source Universe is: " + sourceUniverseID + " - match found - changing universe </br>");

                String xml = getDefaultMapping(reportID, destUniverseID, baseWebiURL, logonToken, nodeID);
                getObjectsFromUniverse(sourceUniverseID, destUniverseID, baseUnvURL, logonToken, xml);
                doChange(reportID, destUniverseID, baseWebiURL, logonToken, nodeID, xml);
                commitChange(reportID, baseWebiURL, logonToken);
            }
        }

        doLogoff(logoffURL, logonToken);


    }

    private static void getObjectsFromUniverse(String sourceUniverseID, String destUniverseID, String baseWebiURL, String logonToken, String mappingXml) throws Exception {
        String unvURL = baseWebiURL + "/"+ destUniverseID;
        String unvXML = WSHelper.restGet(unvURL, "X-SAP-LogonToken", logonToken, "", "");
        Document targetUnvDom = WSHelper.convertStringToDom(unvXML);

        unvURL = baseWebiURL + "/"+ sourceUniverseID;
        unvXML = WSHelper.restGet(unvURL, "X-SAP-LogonToken", logonToken, "", "");
        Document srcUnvDom = WSHelper.convertStringToDom(unvXML);

        Document mappingDom = WSHelper.convertStringToDom(mappingXml);
        NodeList sourceList = mappingDom.getElementsByTagName("mapping");



        for (int j = 0; j < sourceList.getLength(); j++) {
            Element dpElement = (Element) sourceList.item(j);
            String status = dpElement.getAttribute("status");
            String srcText = dpElement.getElementsByTagName("source").item(0).getTextContent();
            String targetText = dpElement.getElementsByTagName("target").item(0).getTextContent();

            String sourceID = parseDataSourceID(srcText);
            String targetID = parseDataSourceID(targetText);

            String sourceLabel = getUniverseItemName(srcUnvDom, sourceID);
            String tagetLabel = getUniverseItemName(targetUnvDom, targetID);

            System.out.println("status is " + status);
            System.out.println(" source: " + sourceID + ": " + sourceLabel);
            System.out.println(" destination: " + targetID + ": " + tagetLabel);
            System.out.println();
        }



       // System.out.println("Destination Universe Structure is:\n" + unvXML);

    }

    private static String getUniverseItemName(Document unvDom, String srcID) {
        String label = "";
        NodeList unvItemList = unvDom.getElementsByTagName("item");

        for (int j = 0; j < unvItemList.getLength(); j++) {
            Element dpElement = (Element) unvItemList.item(j);
            NodeList ns = dpElement.getElementsByTagName("id");
            String id = ns.item(0).getTextContent();
            if(srcID.equals(id)) {
                NodeList nameNode = dpElement.getElementsByTagName("name");
                label = nameNode.item(0).getTextContent();
            }


        }
        return label;
    }

    private static String parseDataSourceID (String in) {
        String out = in.trim();
        if(out.contains(".")) {
            out = out.substring(out.lastIndexOf(".")+1);
        }
        return out;
    }

    private static void doLogoff(String logoffURL, String logonToken) throws Exception {
        String xmlString = WSHelper.restPost(logoffURL, "", "X-SAP-LogonToken", logonToken, "", "");


        System.out.println("Logged off - done");
    }

    private static void commitChange(String reportID, String baseWebiURL, String logonToken) throws Exception {
        String putUrl = baseWebiURL + "/" + reportID;

        String xmlString5 = WSHelper.restPut(putUrl, "", "X-SAP-LogonToken", logonToken, "", "");
    }

    private static String getDefaultMapping(String reportID, String destUniverseID, String baseWebiURL, String logonToken, String nodeID) throws Exception {
        // First get the default mappings
        String changeUnivUrl = baseWebiURL + "/" + reportID + "/dataproviders/mappings?originDataproviderIds=" + nodeID + "&targetDatasourceId=" + destUniverseID;
        String xmlString3 = WSHelper.restGet(changeUnivUrl, "X-SAP-LogonToken", logonToken, "", "");
        System.out.println("Default mapping: " + xmlString3 + "");
        return xmlString3;

    }

    private static void doChange(String reportID, String destUniverseID, String baseWebiURL, String logonToken, String nodeID, String xmlString) throws Exception {
        String changeUnivUrl = baseWebiURL + "/" + reportID + "/dataproviders/mappings?originDataproviderIds=" + nodeID + "&targetDatasourceId=" + destUniverseID;
        // Now do the actual change
        String xmlString4 = WSHelper.restPost(changeUnivUrl, xmlString, "X-SAP-LogonToken", logonToken, "", "");
        System.out.println("Result of call: " + xmlString4 + "");
    }

    private static String getUniverseForDataProvider(String reportID, String baseWebiURL, String logonToken, String nodeID) throws Exception {
        // Now we need to check what universe this dataprovider is based off of.  To do that, we need to do a second query
        String xmlString2 = WSHelper.restGet(baseWebiURL + "/" + reportID + "/dataproviders/" + nodeID, "X-SAP-LogonToken", logonToken, "", "");
        System.out.println(xmlString2);
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
