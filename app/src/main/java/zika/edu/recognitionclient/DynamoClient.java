package zika.edu.recognitionclient;

import android.content.Context;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;

public class DynamoClient {

    private static AmazonDynamoDBClient sDynamoDBClient;

    private DynamoClient() {}

    public static AmazonDynamoDBClient getClient(Context ctx) {
        if(sDynamoDBClient == null) {
            AWSCredentials AWSCreds = new AWSCredentials() { //Local Dynamo doesn't care about creds
                @Override
                public String getAWSAccessKeyId() {
                    return "anything";
                }

                @Override
                public String getAWSSecretKey() {
                    return "anything";
                }
            };
            sDynamoDBClient = new AmazonDynamoDBClient(AWSCreds);
            sDynamoDBClient.setEndpoint("http://localhost:5000/"); //Change this from localhost if deploying to AWS
            sDynamoDBClient.setRegion(Region.getRegion(Regions.US_EAST_2));
            sDynamoDBClient.setSignerRegionOverride("us-east-2");
        }

        return sDynamoDBClient;
    }

}
