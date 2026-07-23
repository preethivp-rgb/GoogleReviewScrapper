package com.yosi.reviewcomparator;

import com.yosi.reviewcomparator.batch.Location;
import com.yosi.reviewcomparator.batch.LocationsExcel;

import java.util.List;

public class DebugCountLocations {
    public static void main(String[] args) throws Exception {
        Config config = new Config();
        List<Location> locations = LocationsExcel.read(config.locationsInputPath());
        System.out.println("Total locations parsed: " + locations.size());
        System.out.println("First: practice_id=" + locations.get(0).practiceId
                + " url=" + locations.get(0).googleMapsUrl);
        System.out.println("Last: practice_id=" + locations.get(locations.size() - 1).practiceId
                + " url=" + locations.get(locations.size() - 1).googleMapsUrl);
    }
}
