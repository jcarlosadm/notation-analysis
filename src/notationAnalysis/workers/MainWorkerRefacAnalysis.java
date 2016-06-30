package notationAnalysis.workers;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Hashtable;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;

import gitCommitStatistics.git.GitManager;
import gitCommitStatistics.properties.PropertiesManager;
import gitCommitStatistics.workers.MainWorker;
import gitCommitStatistics.workers.Worker;
import notationAnalysis.util.Report;

public class MainWorkerRefacAnalysis extends MainWorker {

    private static final String REPORT_PATH = "report.txt";

    private static final double TOLERANCE_LEVEL = 0.8;

    private static final String DISCIPLINED_STRING = "disciplined";

    private static final String UNDISCIPLINED_STRING = "undisciplined";

    private static final int DISCIPLINED_INDEX = 1;

    private static final int UNDISCIPLINED_INDEX = 0;

    private static final String NOTATION_SEPARATOR = "@";

    private static final String UNDISCIPLINED_MARK = "u";

    private static MainWorkerRefacAnalysis instance = null;

    public static MainWorkerRefacAnalysis getInstance() {
        if (instance == null) {
            instance = new MainWorkerRefacAnalysis();
        }

        return instance;
    }

    @Override
    protected Worker getWorkerInstance(ArrayList<String> commits, Hashtable<String, ArrayList<String>> changeMap,
            int commitIndex) {
        return new WorkerRefacAnalysis("Worker-" + commitIndex % numberOfWorkers, commits.get(commitIndex),
                changeMap.get(commits.get(commitIndex)));
    }

    @Override
    protected boolean writeResults(String repo) {
        return true;
    }

    @Override
    public void createBackup() {
    }

    @Override
    protected void makeAnalysisOnCurrentRepo(GitManager gitManager) {

        Report report = Report.getInstance(REPORT_PATH);
        if (report == null) {
            System.out.println("error to create report");
            return;
        }

        List<String> commits = gitManager.getCommitHashList();
        Collections.reverse(commits);

        int commitIndex = 0;
        String commitId = commits.get(commitIndex);
        Hashtable<String, ArrayList<String>> fileHash = null;

        List<String> undisciplinedList = new ArrayList<>();
        Hashtable<String, List<String>> hashNotationsOther = null;

        while (commitIndex < (commits.size() - 1)) {
            commitId = commits.get(commitIndex);

            if (!this.resultMap.containsKey(commitId)) {
                ++commitIndex;
                continue;
            }

            fileHash = this.resultMap.get(commitId);
            for (String file : fileHash.keySet()) {

                BufferedReader bReader = this.createBufferedReader(file, commitId);
                if (bReader == null) {
                    continue;
                }

                List<String> head = fileHash.get(file);
                List<String> body = new ArrayList<>();
                String line = "";
                try {
                    while ((line = bReader.readLine()) != null) {
                        body.add(line);
                    }

                    bReader.close();
                } catch (Exception e) {
                    System.out.println("error to read file");
                    continue;
                }

                undisciplinedList.clear();
                this.addUndisciplinedList(undisciplinedList, body);

                if (!this.checkUndNotationTotal(head, undisciplinedList)) {
                    System.out.println("undisciplined total don\'t match");
                    continue;
                }

                String commitIdOther = commits.get(commitIndex + 1);
                if (!this.resultMap.containsKey(commitIdOther)
                        || !this.resultMap.get(commitIdOther).containsKey(file)) {
                    continue;
                }

                BufferedReader bReader2 = this.createBufferedReader(file, commitIdOther);
                if (bReader2 == null) {
                    continue;
                }

                List<String> headOther = this.resultMap.get(commitIdOther).get(file);
                List<String> bodyOther = new ArrayList<>();
                line = "";
                try {
                    while ((line = bReader2.readLine()) != null) {
                        bodyOther.add(line);
                    }

                    bReader.close();
                } catch (Exception e) {
                    System.out.println("error to read file");
                    continue;
                }

                hashNotationsOther = new Hashtable<>();
                this.fillDisciplined(hashNotationsOther, bodyOther);
                this.fillUndisciplined(hashNotationsOther, bodyOther);

                if (!this.checkNotationNumbers(headOther, hashNotationsOther)) {
                    System.out.println("disciplined and undisciplined total don\'t match");
                    continue;
                }

                boolean found = false;
                String auxString = "";
                for (String undNotation : undisciplinedList) {
                    if (found)
                        found = false;

                    for (String undNotationOther : hashNotationsOther.get(UNDISCIPLINED_STRING)) {
                        if (undNotation.equals(undNotationOther)) {
                            found = true;
                            auxString = undNotationOther;
                            break;
                        }
                    }

                    if (found) {
                        hashNotationsOther.get(UNDISCIPLINED_STRING).remove(auxString);
                        continue;
                    }

                    for (String dNotationOther : hashNotationsOther.get(DISCIPLINED_STRING)) {
                        if (this.compare(undNotation, dNotationOther) >= TOLERANCE_LEVEL) {
                            // TODO write report
                            try {
                                report.write(commitId + " " + commitIdOther + " ");
                                report.writeNewline();
                                report.write(undNotation);
                                report.writeNewline();
                                report.write(dNotationOther);
                                report.writeNewline();
                            } catch (IOException e) {
                                System.out.println("error to write report");
                            }

                            auxString = dNotationOther;
                            found = true;
                            break;
                        }
                    }

                    if (found) {
                        hashNotationsOther.get(DISCIPLINED_STRING).remove(auxString);
                        continue;
                    }
                }
            }

            ++commitIndex;
        }

        if (!report.close())
            System.out.println("error to close report");

        this.deleteAndCreateBackupFolder();

        System.out.println("finished analysis");

    }

    private void deleteAndCreateBackupFolder() {
        String backupPath = PropertiesManager.getPropertie("path") + System.getProperty("file.separator") + "backup";
        File backupFolder = new File(backupPath);
        if (backupFolder.exists() && backupFolder.isDirectory()) {
            try {
                FileUtils.deleteDirectory(backupFolder);
            } catch (Exception e) {
                System.out.println("error to delete backup folder");
            }
        }
        backupFolder.mkdir();
    }

    private BufferedReader createBufferedReader(String file, String commit) {
        String resultFolderPath = PropertiesManager.getPropertie("path") + System.getProperty("file.separator")
                + "backup" + System.getProperty("file.separator") + commit;
        File backupFolder = new File(resultFolderPath);
        if (!backupFolder.exists() || !backupFolder.isDirectory()) {
            System.out.println("backup folder not exists");
            return null;
        }

        String filename = file.substring(0, file.lastIndexOf("."));
        String filepath = resultFolderPath + System.getProperty("file.separator") + filename;
        BufferedReader bReader = null;
        try {
            bReader = new BufferedReader(new FileReader(new File(filepath)));
        } catch (FileNotFoundException e1) {
            System.out.println("error to read file");
            return null;
        }

        return bReader;
    }

    private boolean checkUndNotationTotal(List<String> headValues, List<String> undisciplinedList) {
        int totalUndDisciplined = this.getTotalUndisciplined(headValues);

        return (totalUndDisciplined == undisciplinedList.size());
    }

    private void addUndisciplinedList(List<String> undisciplinedList, List<String> body) {
        undisciplinedList.clear();
        undisciplinedList.addAll(this.getNotationList(body, UNDISCIPLINED_STRING));
    }

    /**
     * Get similarity between two strings. Algorithm found in
     * http://stackoverflow.com/questions/955110/similarity-string-comparison-in
     * -java
     * 
     * @param str1
     *            String one
     * @param str2
     *            String two
     * @return similarity level between 0 and 1, inclusive
     */
    private double compare(String str1, String str2) {
        // TODO implement
        String longer = str1, shorter = str2;
        if (str1.length() < str2.length()) { // longer should always have
                                             // greater length
            longer = str2;
            shorter = str1;
        }
        int longerLength = longer.length();
        if (longerLength == 0) {
            return 1.0;
            /* both strings are zero length */ }
        /*
         * // If you have StringUtils, you can use it to calculate the edit
         * distance: return (longerLength -
         * StringUtils.getLevenshteinDistance(longer, shorter)) / (double)
         * longerLength;
         */
        return (longerLength - StringUtils.getLevenshteinDistance(longer, shorter)) / (double) longerLength;
    }

    private boolean checkNotationNumbers(List<String> headValues, Hashtable<String, List<String>> hashNotations) {
        int totalDisciplined = this.getTotalDisciplined(headValues);
        int totalUndisciplined = this.getTotalUndisciplined(headValues);

        boolean matchUnd = (totalUndisciplined == hashNotations.get(UNDISCIPLINED_STRING).size());
        boolean matchD = (totalDisciplined == hashNotations.get(DISCIPLINED_STRING).size());

        return (matchD && matchUnd);
    }

    private int getValueFromHead(List<String> head, int index) {
        int value = 0;
        try {
            value = Integer.parseInt(head.get(index));
        } catch (Exception e) {
            value = 0;
        }

        return value;
    }

    private int getTotalUndisciplined(List<String> head) {
        return this.getValueFromHead(head, UNDISCIPLINED_INDEX);
    }

    private int getTotalDisciplined(List<String> head) {
        return this.getValueFromHead(head, DISCIPLINED_INDEX);
    }

    private List<String> getNotationList(List<String> body, String notationType) {
        List<String> list = new ArrayList<>();

        boolean undisciplined = false;
        boolean disciplined = false;
        if (notationType.equals(UNDISCIPLINED_STRING)) {
            undisciplined = true;
        } else {
            disciplined = true;
        }

        List<String> lines = body;
        boolean found = false;
        String auxString = "";
        for (String line : lines) {
            if (found && line.trim().startsWith(NOTATION_SEPARATOR)) {
                found = false;
                if (auxString != null && !auxString.isEmpty()) {
                    list.add(auxString);
                }
                auxString = "";
            }

            if (!found && line.trim().startsWith(NOTATION_SEPARATOR)
                    && ((undisciplined == true && line.toLowerCase().contains(UNDISCIPLINED_MARK))
                            || (disciplined == true && !line.toLowerCase().contains(UNDISCIPLINED_MARK)))) {
                found = true;
            } else if (found == true) {
                auxString += line + System.getProperty("line.separator");
            }
        }
        if (found && auxString != null && !auxString.isEmpty()) {
            list.add(auxString);
        }

        return list;
    }

    private void fillUndisciplined(Hashtable<String, List<String>> hashNotations, List<String> body) {
        hashNotations.put(UNDISCIPLINED_STRING, this.getNotationList(body, UNDISCIPLINED_STRING));
    }

    private void fillDisciplined(Hashtable<String, List<String>> hashNotations, List<String> body) {
        hashNotations.put(DISCIPLINED_STRING, this.getNotationList(body, DISCIPLINED_STRING));
    }
}
