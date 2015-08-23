package com.metaminer;

public class Main {


    public static void main(String[] args) {

        // Enterprise Authentication Credentials
        String boUsername = "Administrator";
        String boPassword = "ph!l!!";

        // Sample Variables
        String reportID = "55106";
        String sourceUniverseID = "54780"; // Only change dataproviders based off of this universe
        String destUniverseID = "54756";  // Change those dataproviders to use this Universe

        // Restful URL's
        final String serverPort = "localhost:6427";

        try{
            ReMapUniverse.remap(serverPort,boUsername,boPassword,reportID,sourceUniverseID,destUniverseID);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }



}