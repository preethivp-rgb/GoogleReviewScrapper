package com.yosi.reviewcomparator;

import com.yosi.reviewcomparator.batch.LocationsExcel;

public class GenerateTemplate {
    public static void main(String[] args) throws Exception {
        Config config = new Config();
        LocationsExcel.generateTemplateIfMissing(config.locationsInputPath());
        System.out.println("Template written to: " + config.locationsInputPath());
    }
}
