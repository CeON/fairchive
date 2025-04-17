package edu.harvard.iq.dataverse.persistence.geonames;

import java.io.FileInputStream;
import java.io.InputStream;

public class GeoNamesImporterTest {
    
    public static void main(String[] args) throws Exception {

//        System.out.println("Press any key");
//        System.in.read();
        
        try (final InputStream in = new FileInputStream("C:\\prj\\dariah\\geonames\\US.txt")) {
            GeoNamesImporter.readNames(in);
        }

    }
}
