package zika.edu.recognitionclient.model;

import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.DynamoDBAttribute;
import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.DynamoDBHashKey;
import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.DynamoDBRangeKey;
import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.DynamoDBTable;

import java.nio.ByteBuffer;

@DynamoDBTable(tableName = "Photos")
public class Photo {
    private String username;
    private String timestamp;
    //    private String image_url;
    private ByteBuffer imageUrl;
    private double latitude;
    private double longitude;
    private double last_known_lat;
    private double last_known_long;

    @DynamoDBHashKey(attributeName = "username")
    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    @DynamoDBRangeKey(attributeName = "timestamp")
    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

//    @DynamoDBAttribute(attributeName = "image_url")
//    public String getimageUrl() {
//        return imageUrl;
//    }
//
//    public void setimageUrl(String image_url) {
//        this.imageUrl = image_url;
//    }

    @DynamoDBAttribute(attributeName = "image_url")
    public ByteBuffer getimageUrl() {
        return imageUrl;
    }

    public void setimageUrl(ByteBuffer image_url) {
        this.imageUrl = image_url;
    }

    @DynamoDBAttribute(attributeName = "latitude")
    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    @DynamoDBAttribute(attributeName = "longitude")
    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    @DynamoDBAttribute(attributeName = "last_known_lat")
    public double getLast_known_lat() {
        return last_known_lat;
    }

    public void setLast_known_lat(double last_known_lat) {
        this.last_known_lat = last_known_lat;
    }

    @DynamoDBAttribute(attributeName = "last_known_long")
    public double getLast_known_long() {
        return last_known_long;
    }

    public void setLast_known_long(double last_known_long) {
        this.last_known_long = last_known_long;
    }
}
