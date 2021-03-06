package at.webCrawler.parsers;


import at.webCrawler.CrawlerBehaviour;
import org.apache.http.HttpStatus;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class RobotstxtParser {

    /**
     * Mainmethod for parsing Robots.txt. From here the different methods are executed.
     *
     * @param base Link to website/source of the robots.txt
     */
    public static CrawlerBehaviour analyzeRobotsTxt(String base, CrawlerBehaviour currentRobot) {
        //robots.txt von Seite (base) holen
        String inputFile = getRobotsTxtFile(base);
        //crawlerspezifische Anweisungen von einander trennen
        List<String> groupList = searchAndCreateGroups(inputFile, "User-agent:");

        //-crawlerspezifische Anweisungen filtern
        for (String x : groupList) {
//            System.out.println("analyze: " + x);
            if (x.startsWith("AFFE") || x.startsWith(" AFFE")) {
                //Anweisungen für AFFE auflisten
                //Anweisungen anhand linereturn aufteilen
                splitRobotsTxt(x, currentRobot);
                //aufgeteilte Anweisungen filtern und verarbeiten
            } else if (x.startsWith("*") || x.startsWith(" *")) {
                //Anweisungen für alle crawler auflisten
                //Anweisungen anhand linereturn aufteilen
                splitRobotsTxt(x, currentRobot);
                //aufgeteilte Anweisungen filtern und verarbeiten
            }
        }
        return currentRobot;
    }

    /**
     * uses the java.net.HttpClient to receive the robots.txt from a website.
     *
     * @param base Link to websites robots.txt
     * @return robots.txt as a String
     */
    private static String getRobotsTxtFile(String base) {
        //-Client für Webdateien
        HttpClient client = HttpClient.newBuilder()
                .version(java.net.http.HttpClient.Version.HTTP_1_1)
                .followRedirects(java.net.http.HttpClient.Redirect.NORMAL)
                .connectTimeout(Duration.ofSeconds(20))
                .build();
        //URL Sachen
        URI robotsTxtUri = URI.create("http://www.beispieluri.com");
        String robotsTxtContent = "";

        //robots.txt
        //-robots.txt Anfrage vorbereiten
        //--URL zur robots.txt erzeugen
        robotsTxtUri = URI.create(base + "/robots.txt");
//            System.out.println("robots.txt URI = " + robotsTxtUri);
        //--robots.txt Anfrage beschreiben
        HttpRequest request = HttpRequest.newBuilder()
                .uri(robotsTxtUri)
                .timeout(Duration.ofMinutes(2))
                .build();
        try {
            //-robots.txt Anfrage durchführen (+Auswahl Rückgabeform)
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            //-robots.txt Anfrage Erfolg prüfen und Ergebnis verarbeiten
            if (response.statusCode() == HttpStatus.SC_OK) {
//                System.out.println("Ziel :" + robotsTxtUri);

                if (response.body().isEmpty() || response.body().isBlank() || response.body().length() < 1) {
                    //wenn response leer ist muss trotzdem etwas in blacklist stehen da diese sonst null zurückkommt
                    //TODO: Seiten ohne robots.txt und falsche robots.txt verarbeiten
                    robotsTxtContent = "Seite hat keine oder fehlerhafte robots.txt. \n" +
                            "User-agent: * \n" +
                            "Crawl-delay: 0 \n" +
                            "Disallow: 000000000";
                } else {
                    //--robots.txt in String füllen
                    robotsTxtContent = response.body();
                }
            } else if (response.statusCode() == HttpStatus.SC_REQUEST_TIMEOUT) {
                robotsTxtContent = createAbortRobot(base);
            }
        }  catch (IllegalArgumentException iae) {
            robotsTxtContent = createAbortRobot(base);
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
//        System.out.println(robotsTxtContent);
        return robotsTxtContent;
    }

    /**
     * Splits a String into parts. Every Part is indicated by the separator.
     *
     * @param fullTxt   the entire string which needs to be split into groups
     * @param separator indicates the beginning and end of a group
     * @return the List of groups found
     */
    private static List<String> searchAndCreateGroups(String fullTxt, String separator) {
        String inputText = fullTxt;
        String txtGroup = "";
        ArrayList<String> groupList = new ArrayList<>();

        String[] orderList = inputText.split(separator);
        //String[] orderList = inputText.split(("(?i)User-Agent"));
        for (int i = 0; i < orderList.length; i++) {
            txtGroup = orderList[i];
            groupList.add(txtGroup);
//            System.out.println("*****************************************");
//            System.out.println("Anweisung #" + i + " : ");
//            System.out.println(txtGroup);
        }
        return groupList;
    }

    /**
     * Splits String into separate Strings for each line. Every line gets analyzed for its content.
     * It fills relevant context of the robots.txt into the fields of the class object and returns the class object.
     *
     * @param robotsTxtContent String that needs to be split per line.
     * @return Classobject of CrawlerBehaviour
     */
    private static CrawlerBehaviour splitRobotsTxt(String robotsTxtContent, CrawlerBehaviour currentRobot) {
        Scanner scanner = new Scanner(robotsTxtContent);
        ArrayList<String> blacklist = new ArrayList<String>();
        while (scanner.hasNextLine()) {
            String line = scanner.nextLine();
            if (line.isEmpty()) {
                continue;
            }
//          analyzeLineDisallow(line);
            if (line.toLowerCase().startsWith("disallow")) {
                //CrawlerBehaviour Class objekt: array element mit disallow entry füllen
                //TODO: Class object befüllen anstatt der ArrayList welche später an die class übergeben wird.
                // Wenn die Beschreibung so kompliziert ist, ist es der Vorgang auch.
                blacklist.add(analyzeLineDisallow(line));
//                currentRobot.getSiteBlacklist().add(analyzeLineDisallow(line));

            }
            if (line.toLowerCase().startsWith("crawl-delay:")) {
                //CrawlerBehaviour Class objekt: delay füllen
                currentRobot.setDelay(analyzeLineDelay(line));
            }
        }
        scanner.close();
        //CrawlerBehaviour Class objekt: zurückgeben an aufrufende methode
        //-Ziel-ArrayList durch Hilfs-ArrayList befüllen
        currentRobot.setSiteBlacklist(blacklist);
        return currentRobot;
    }

    /**
     * Analyzes a line of the robots.txt to check what the content wants to control.
     * Should only handle lines that are relevant for us or every crawler.
     *
     * @param line a single line of the robots.txt
     * @return value of the disallow-line
     */
    private static String analyzeLineDisallow(String line) {
        //unformatierter Eintrag
        String entry = "";

        if (line.toLowerCase().startsWith("disallow")) {
            //Pfad welcher nicht erlaubt ist
            entry = line.substring(9).trim();
            //Form der Pfadangabe verarbeiten
            // */entry
            if (entry.startsWith("*")) {
//                System.out.println("So gehts rein: " + entry);
                entry = entry.replace("*", "");
//                System.out.println("So kommts raus: " + entry);
            }
            // /*entry
            if (entry.startsWith("/*")) {
//                System.out.println("So gehts rein: " + entry);
                entry = entry.replace("/*", "");
//                System.out.println("So kommts raus: " + entry);
            }
            // /entry*
            if (entry.endsWith("*")) {
//                System.out.println("So gehts rein: " + entry);
                entry = entry.replace("*", "");
//                System.out.println("So kommts raus: " + entry);
            }
            if (entry.endsWith("$")) {
//                System.out.println("So gehts rein: " + entry);
                entry = entry.replace("$", "");
//                System.out.println("So kommts raus: " + entry);
            }
//            System.out.println("***********************************");
        }
        return entry;
    }

    private static int analyzeLineDelay(String line) {
        int delay = 0;
        //delay ermitteln
//      System.out.println("delay : " + line);
        delay = Integer.parseInt(line.substring(12).trim());
//      System.out.println("delay bekannt : " + delay);
        return delay;
    }

    private static String[] whateverRobotBlacklist(String robotsTxt) {

        String[] robotBlacklist = new String[]{};

        //Zeilenweise Anweisungen aufarbeiten
        //keine robots.txt oder robots.txt leer
        if ((robotsTxt == null) || (robotsTxt.length() == 0)) {
            //alles erlaubt
        }
        //erste line enthält <!document HTML dingsda ===> das ist kein JimBeam

        return robotBlacklist;
    }

    private static String createAbortRobot(String base) {
        String abortRobotTxt = "* Seite nicht erreicht. Verarbeitung abgebrochen. \n" +
                "User-agent: * \n" +
                "Crawl-delay: 0 \n" +
                "Disallow: " + base;
        return abortRobotTxt;
    }

}
