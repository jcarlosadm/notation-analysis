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
import notationAnalysis.util.Counters;
import notationAnalysis.util.Report;
import notationAnalysis.util.jplag.CompareJplag;

public class MainWorkerRefacAnalysis extends MainWorker {

    private static final String REPORT_FOLDER_NAME = "reports";

    private static final String REPORT_NAME = "report";
    private static final String REPORT_EXTENSION = "txt";

    private static final String DISCIPLINED_STRING = "disciplined";

    private static final String UNDISCIPLINED_STRING = "undisciplined";

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
    protected boolean makeClone(String repoName) {

        String path = GitManager.PROJECT_PATH + File.separator + repoName + File.separator + ".git";
        File gitFile = new File(path);
        if (gitFile.exists() && gitFile.isDirectory()) {
            return false;
        }

        return true;
    }

    @Override
    protected void fillResultMap() {
        String backupProjectFolderPath = PropertiesManager.getPropertie("path") + File.separator + "backup"
                + File.separator + MainWorker.getCurrentRepoName();
        File backupProjectFolder = new File(backupProjectFolderPath);
        if (!backupProjectFolder.exists() || !backupProjectFolder.isDirectory()) {
            return;
        }

        File[] folders = backupProjectFolder.listFiles();
        for (File folder : folders) {
            if (folder.isDirectory()) {
                this.resultMap.put(folder.getName(), new Hashtable<String, ArrayList<String>>());

                File[] files = folder.listFiles();
                for (File file : files) {
                    if (!file.isDirectory() && !file.getName().endsWith("SSSSerrorSSSS")) {
                        this.resultMap.get(folder.getName()).put(file.getName() + ".c", new ArrayList<String>());
                    }
                }
            }
        }
    }

    @Override
    protected boolean getData() {
        String property = PropertiesManager.getPropertie("reuse.dmacros.data");

        boolean reuse;
        try {
            reuse = Boolean.parseBoolean(property);
        } catch (Exception e) {
            return true;
        }

        File dataPath = new File(PropertiesManager.getPropertie("path") + File.separator + "backup" + File.separator
                + MainWorker.getCurrentRepoName());
        if (!dataPath.exists() || !dataPath.isDirectory()) {
            return true;
        }

        if (!reuse) {
            try {
                FileUtils.deleteDirectory(dataPath);
            } catch (IOException e) {
            }

            return true;
        }

        return false;
    }

    @Override
    protected void makeAnalysisOnCurrentRepo(GitManager gitManager, String repo) {

        if (!this.createReportFolder()) {
            return;
        }

        String reponame = repo.substring(repo.lastIndexOf(File.separator) + 1, repo.lastIndexOf("."));

        String reportPath = PropertiesManager.getPropertie("path") + File.separator + REPORT_FOLDER_NAME
                + File.separator + REPORT_NAME + "_" + reponame + "." + REPORT_EXTENSION;
        Report report = Report.getInstance(reportPath);
        if (report == null) {
            System.out.println("error to create report");
            return;
        }

        List<String> commits = new ArrayList<>();
        List<String> completeCommits = gitManager.getCommitHashList();
        Collections.reverse(completeCommits);

        for (String cCommit : completeCommits) {
            if (this.resultMap.containsKey(cCommit)) {
                commits.add(cCommit);
            }
        }

        int commitIndex = 0;
        if (commits.size() <= 0) {
            this.finishAnalysis(report);
            return;
        }

        String commitId = commits.get(commitIndex);
        Hashtable<String, ArrayList<String>> fileHash = null;

        List<String> undisciplinedList = new ArrayList<>();
        Hashtable<String, List<String>> hashNotationsOther = null;

        double toleranceLevel = 0.5;
        try {
            toleranceLevel = Double.parseDouble(PropertiesManager.getPropertie("comparator.tolerance.level"));
        } catch (Exception e) {
            toleranceLevel = 0.5;
        }

        Counters notationCounters = new Counters();
        notationCounters.setNumberOfCommits(commits.size());

        while (commitIndex < (commits.size() - 1)) {
            commitId = commits.get(commitIndex);

            if (!this.resultMap.containsKey(commitId)) {
                ++commitIndex;
                continue;
            }

            fileHash = this.resultMap.get(commitId);
            boolean commitLock = false;
            // progress
            System.out.printf("Progress: %.2f %%\n", notationCounters.getProgress());
            System.out.println("commit: " + commitId);

            for (String file : fileHash.keySet()) {
                System.out.printf("    Progress: %.2f %%\n", notationCounters.getProgress());
                System.out.println("    file: " + file);

                BufferedReader bReader = this.createBufferedReader(file, commitId);
                if (bReader == null) {
                    continue;
                }

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

                undisciplinedList = this.getNotationList(body, UNDISCIPLINED_STRING);

                if ((!notationCounters.getCommits().isEmpty() && !notationCounters.getCommits().contains(commitId))
                        || commitLock == true) {
                    commitLock = true;
                    notationCounters.addUndisciplinedTotal(commitId, undisciplinedList.size());
                    notationCounters.addDisciplinedTotal(commitId,
                            this.getNotationList(body, DISCIPLINED_STRING).size());
                }

                String commitIdOther = commits.get(commitIndex + 1);
                if (!this.resultMap.containsKey(commitIdOther)
                        || !this.resultMap.get(commitIdOther).containsKey(file)) {
                    continue;
                }
                System.out.println("      current commit: " + commitId);
                System.out.println("      next commit: " + commitIdOther);

                BufferedReader bReader2 = this.createBufferedReader(file, commitIdOther);
                if (bReader2 == null) {
                    continue;
                }

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

                notationCounters.addUndisciplinedTotal(commitIdOther,
                        hashNotationsOther.get(UNDISCIPLINED_STRING).size());
                notationCounters.addDisciplinedTotal(commitIdOther, hashNotationsOther.get(DISCIPLINED_STRING).size());

                boolean found = false;
                String auxString = "";
                for (String undNotation : undisciplinedList) {
                    System.out.print("*");
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

                    CompareJplag cJplag = CompareJplag.getInstance();
                    for (String dNotationOther : hashNotationsOther.get(DISCIPLINED_STRING)) {

                        // double comp = compare(undNotation, dNotationOther);
                    	
                    	double comp = cJplag.compareTo(undNotation, dNotationOther);
                        if (comp >= toleranceLevel) {
                            try {
                                System.out.println(" match found");
                                report.write("link: " + this.getCommitUrl(repo, commitIdOther) + " "
                                        + System.lineSeparator());
                                report.write("file: " + file + System.lineSeparator());
                                report.write("similarity: " + comp + System.lineSeparator());
                                report.write("/---------------------------------------------------------\\"
                                        + System.lineSeparator());
                                report.write("******************undisciplined notation******************"
                                        + System.lineSeparator());
                                report.write(undNotation);
                                report.writeNewline();
                                report.write("******************disciplined notation******************"
                                        + System.lineSeparator());
                                report.write(dNotationOther);
                                report.writeNewline();
                                report.write("\\_________________________________________________________/"
                                        + System.lineSeparator());
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
                System.out.println();
            }

            ++commitIndex;
        }

        Report reportCounters = this.createReportCounters(reponame);
        if (reportCounters != null) {
            this.writeReportCounters(reportCounters, notationCounters);
            this.closeReportCounters(reportCounters);
        } else {
            System.out.println("error to create report for notation counters");
        }

        this.finishAnalysis(report);
    }

    /**
     * Get url to commit of repository
     * 
     * @param repo
     *            repository url
     * @param commitId
     *            commit hash
     * @return url to commit of repository
     */
    private String getCommitUrl(String repo, String commitId) {
        return repo.substring(0, repo.lastIndexOf(".git")) + "/commit/" + commitId + "?diff=split";
    }

    private Report createReportCounters(String reponame) {
        String reportCountersPath = PropertiesManager.getPropertie("path") + File.separator + REPORT_FOLDER_NAME
                + File.separator + REPORT_NAME + "_" + reponame + "_counters." + REPORT_EXTENSION;
        return Report.getInstance(reportCountersPath);
    }

    private void writeReportCounters(Report report, Counters counters) {
        try {
            report.write("undisciplined total = " + counters.getUndisciplinedTotal());
            report.writeNewline();
            report.write("Disciplined total = " + counters.getDisciplinedTotal());
            report.writeNewline();
            report.writeNewline();

            report.write("commits:");
            report.writeNewline();
            for (String commit : counters.getCommits()) {
                report.write(commit + " || disciplined: " + counters.getDisciplinedTotal(commit) + " | undisciplined: "
                        + counters.getUndisciplinedTotal(commit));
                report.writeNewline();
            }
        } catch (IOException e) {
            System.out.println("error to write in report counter");
        }

    }

    private void closeReportCounters(Report reportCounters) {
        if (!reportCounters.close()) {
            System.out.println("error to close report counters");
        }
    }

    private void finishAnalysis(Report report) {
        if (!report.close())
            System.out.println("error to close report");

        boolean recreateBackupFolder;
        try {
            recreateBackupFolder = Boolean.parseBoolean(PropertiesManager.getPropertie("recreate.backup.folder"));
        } catch (Exception e) {
            recreateBackupFolder = true;
        }

        if (recreateBackupFolder)
            this.deleteAndCreateBackupFolder();

        System.out.println("finished analysis");
    }

    private boolean createReportFolder() {
        String resultPath = PropertiesManager.getPropertie("path");
        File resultFolder = new File(resultPath);
        if (!resultFolder.exists() || !resultFolder.isDirectory()) {
            System.out.println("result folder don\'t exists");
            return false;
        }

        String reportFolderPath = resultPath + File.separator + REPORT_FOLDER_NAME;
        File reportFolder = new File(reportFolderPath);
        if (!reportFolder.exists() || !reportFolder.isDirectory()) {
            if (!reportFolder.mkdir()) {
                System.out.println("fail to create report folder");
                return false;
            }
        }

        return true;
    }

    private void deleteAndCreateBackupFolder() {
        String backupPath = PropertiesManager.getPropertie("path") + File.separator + "backup";
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
        String resultFolderPath = PropertiesManager.getPropertie("path") + File.separator + "backup" + File.separator
                + MainWorker.getCurrentRepoName() + File.separator + commit;
        File backupFolder = new File(resultFolderPath);
        if (!backupFolder.exists() || !backupFolder.isDirectory()) {
            System.out.println("backup folder not exists");
            return null;
        }

        String filename = file.substring(0, file.lastIndexOf("."));
        String filepath = resultFolderPath + File.separator + filename;
        BufferedReader bReader = null;
        try {
            bReader = new BufferedReader(new FileReader(new File(filepath)));
        } catch (FileNotFoundException e1) {
            System.out.println("error to read file");
            return null;
        }

        return bReader;
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
    private static double compare(String str1, String str2) {
        str1.replaceAll("\\s+", "");
        str2.replaceAll("\\s+", "");

        String longer = str1, shorter = str2;
        if (str1.length() < str2.length()) { // longer should always have
                                             // greater length
            longer = str2;
            shorter = str1;
        }
        int longerLength = longer.length();
        if (longerLength == 0) {
            return 1.0;
        }
        return (longerLength - StringUtils.getLevenshteinDistance(longer, shorter)) / (double) longerLength;
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
                auxString += line + System.lineSeparator();
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

    /*public static void main(String[] args) {
        // temporary test of similarity function
        String string1_1 = "    int   fd_tmp = mch_open(filename, O_RDONLY" + System.lineSeparator() + "# ifdef WIN32"
                + System.lineSeparator() + "              | O_BINARY | O_NOINHERIT" + System.lineSeparator() + "# endif"
                + System.lineSeparator() + "              , 0);";

        String string1_2 = "# ifdef WIN32" + System.lineSeparator()
                + "    int fd_tmp = mch_open(filename, O_RDONLY | O_BINARY | O_NOINHERIT, 0);" + System.lineSeparator()
                + "# else" + System.lineSeparator() + "    int fd_tmp = mch_open(filename, O_RDONLY, 0);"
                + System.lineSeparator() + "# endif";

        String string2_1 = "    int fd_tmp = mch_open(filename, O_RDONLY" + System.lineSeparator() + "# ifdef WIN32"
                + System.lineSeparator() + "             | O_BINARY | O_NOINHERIT" + System.lineSeparator() + "# endif"
                + System.lineSeparator() + "             , 0);";
        String string2_2 = "# ifdef WIN32" + System.lineSeparator()
                + "    int    fd_tmp = mch_open(filename, O_RDONLY | O_BINARY | O_NOINHERIT, 0);"
                + System.lineSeparator() + "# else" + System.lineSeparator()
                + "    int    fd_tmp = mch_open(filename, O_RDONLY, 0);" + System.lineSeparator() + "# endif";

        String string3_1 = "bool use_curses =" + System.lineSeparator() + "#ifdef HAVE_CURSES" + System.lineSeparator()
                + "   true" + System.lineSeparator() + "#else" + System.lineSeparator() + "   false"
                + System.lineSeparator() + "#endif" + System.lineSeparator() + ";";

        String string3_2 = "#ifdef HAVE_CURSES" + System.lineSeparator() + "bool use_curses = true;"
                + System.lineSeparator() + "#else" + System.lineSeparator() + "bool use_curses;"
                + System.lineSeparator() + "#endif";

        System.out.println("similarity 1 = " + compare(string1_1, string1_2));
        System.out.println("similarity 2 = " + compare(string2_1, string2_2));
        System.out.println("similarity 3 = " + compare(string3_1, string3_2));
    }*/
}
