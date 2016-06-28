package notationAnalysis.util;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class Report {
    
    private BufferedWriter bWriter = null;
    
    private Report(BufferedWriter bWriter) {
        this.bWriter = bWriter;
    }
    
    public static Report getInstance(String path) {
        
        File file = new File(path);
        BufferedWriter bWriter = null;
        try {
            bWriter = new BufferedWriter(new FileWriter(file));
        } catch (IOException e1) {
            System.out.println("error to create report");
            return null;
        }
        
        Report report = new Report(bWriter);
        
        return report;
    }
    
    public boolean close() {
        try {
            this.bWriter.close();
        } catch (IOException e) {
            return false;
        }
        return true;
    }
    
    public void write(String string) throws IOException {
        this.bWriter.write(string);
    }
    
    public void writeNewline() throws IOException {
        this.bWriter.write(System.getProperty("line.separator"));
    }
    
}
