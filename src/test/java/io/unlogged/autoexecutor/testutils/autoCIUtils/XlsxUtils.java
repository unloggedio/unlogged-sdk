package io.unlogged.autoexecutor.testutils.autoCIUtils;

import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.FileInputStream;
import java.net.URL;

public class XlsxUtils {

    public static XSSFWorkbook getWorkbook(URL pathToFile) {
        try {
            XSSFWorkbook workbook = new XSSFWorkbook(
                    new FileInputStream(pathToFile.getPath()));
            return workbook;
        } catch (Exception e) {
            System.out.println("Failed to open test resources, ensure valid file in correct place");
            return null;
        }
    }
}
