package com.yosi.reviewcomparator;

public class Main {

    public static void main(String[] args) throws Exception {
        Config config = new Config();
        SingleLocationRunner.run(config, config.reportOutputPath());
    }
}
