package at.webCrawler;

import at.webCrawler.parsers.KeywordHeadlineParser;
import at.webCrawler.parsers.KeywordMetaParser;
import at.webCrawler.parsers.RobotstxtParser;
import at.webCrawler.parsers.UrlParser;
import at.webCrawler.tool.FileReader;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.DomNode;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import org.w3c.dom.Node;

import java.io.IOException;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * A F F E  -  Automatic File Find Entity
 * Schulprojekt im Rahmen des Digital Campus Vorarlberg - Java Coding Einsteiger;
 * Ersteller:
 * Belal (Programmierung Java);
 * Henning Rüter (Projektleitung, Programmierung Java, Datenbank);
 * Sedat Körpe (Leiter Website Design - Front und Backend);
 * Erstellt: Nov.2020 - Feb.2021;
 * Mitwirkende: Gjula Horvath, Lukas Aichbauer;
 * Unter Verwendung von: JAVA, MySQL, HTMLUnit, HTML;
 * Dank an: Digital Campus Vorarlberg, Stiftung, Arbeitsmarktservice, Oracle, Microsoft, nginx, HTMLUnit, GitHub,
 * Discord, Zoom, Kinder-Beschäftigungsmittelhersteller;
 * Besonderer Dank geht an die Familien und Unterstützer der Beteiligten und sämtliche Ungenannten die
 * an der Erstellung dieses Programms indirekt oder direkt Anteil hatten;
 */
public class Main {
    //*************************************************************************************************
    //
    //                          A F F E  -  Automatic File Find Entity
    //
    //*************************************************************************************************
    // Ersteller:
    // Belal Schenwari
    // Henning Rüter
    // Sedat Körpe
    //
    // Erstellt:
    // Nov.2020 - Feb.2021
    //
    // Mitwirkende:
    // Digital Campus Vorarlberg, Gjula Horvath, Lukas Aichbauer,
    //
    // Unter Verwendung von / Dank geht an:
    // TODO: verwendete Programme und Hilfequellen auflisten
    // TODO: Fördermittelgeber nennen
    //*************************************************************************************************

    //TODO: testing
    //TODO: methode um bevorzugte Seitenziele als erstes zu durchsuchen
    //TODO: methode blacklistWebsite -> welche webseiten nicht besuchen (facebook, linkedIn, google, etc.)
    //TODO: import der ProgrammKlassen oder qualifiziert aufrufen. Aktuell wird beides gemacht. Warum?
    // Was ist besser?
    //TODO: import Node oder DomNode. Alternative siehe KeywordHeaderParser
    //TODO: Erstellung von Logdatei für Fehleraufzeichnung
    public static void main(String[] args) throws IOException {
        //Vorbereitung
        //-Client für Webseiten
        WebClient webClient = new WebClient();

        //-Zähler für Anzahl Programmdurchläufe
        int durchlaeufe = 100;  //gibt Anzahl Durchlaeufe für Programmschleifen vor
        int countReadPages = 0; //zaehlt Durchlaeufe
        boolean stop = false;

        //letztes Ziel
        String lastURL = "";

        //robotsTxt
        //-robotsTxt Class Object erstellen
        //TODO: prüfen: initialisierung mit null ergibt nullpointerException
        //TODO: Versuch: Blacklistarray immer mit einem unsinnigen Wert initialisieren (siehe RobotsTxtParser line83)
        CrawlerBehaviour currentRobot = new CrawlerBehaviour(0, null);
        //-Website Blacklist
        //TODO: NullPointerExeption überwinden

        //**********************************************************************************
        //Programmstart
        while (!stop) {
            System.gc();
            printMemory();
            //nächstes Crawler-Ziel wählen
            //-Ziele erstes mal besuchen
            String nextURL = DataBaseFunction.readDB_nextTarget();
            //-Ziele manuell vorgeben
            //TODO: methode schreiben um txt Datei einzulesen
//            String nextURL = "https://www.laendlejob.at/";

            //-Ziele erneut besuchen
            //TODO: nextvisit berücksichtigen wenn Internet vollständig erfasst
            if (nextURL.length() < 1) {
                System.out.println("Das Internet wurde aufgezeichnet.");
                //TODO: nextvisit SQL anfrage
                nextURL = DataBaseFunction.readDB_nextTargetVisit();
                System.out.println("Neue Ziele sind alte Ziele : " + nextURL);
            }
            //testURLs
//            String nextURL = "https://www.wetator.org/";
//            String nextURL = "https://www.github.com/search";
//            String nextURL = "https://github.com/Eishenne/AFFE.git/download";
            int targetId = getTargetId(nextURL);

            System.out.println("Nächstes Ziel: " + nextURL);
            //Robots.Txt Link erzeugen und abrufen
            //TODO: 01 try catch löschen
            try {
                RobotstxtParser.analyzeRobotsTxt(getHostUrl(nextURL), currentRobot);
            } catch (NullPointerException npe) {
                System.out.println("ClassObject mit null initialisiert, fix it!");
            }
            //TODO: 01
            //analyzeRobotsTxt return auswerten
            //TODO: deaktiviert wegen NullPointerExeption
            boolean inBlacklist = false;
            for (int i = 0; i < currentRobot.getSiteBlacklist().size(); i++) {
                if (nextURL.contains(currentRobot.getSiteBlacklist().get(i))) {
                    //Seite befindet sich in robots.txt blacklist, Seite mit Datum versehen und nicht aufrufen
                    System.out.println("Verarbeitung der Seite durch Betreiber nicht gewünscht.");
                    //nextvisit aufrufen und mit datum versehen
                    DataBaseFunction.updateTargetNextVisit(targetId, "Verarbeitung nicht erwünscht",
                            "Verarbeitung nicht erwünscht");
                    inBlacklist = true;
                    break;
                }
            }
            if (inBlacklist) {
                continue;
            }

//                System.out.println(currentRobot.getSiteBlacklist().get(i));
            // !!!!!!!!!!!!!! liefert 1 Treffer wenn * durch % ersetzt werden und 2 wenn sie entfernt werden
            // !!!!!!!!!!!!!! /downloads liefert treffer wenn entfernt und keinen wenn /*/ durch /%/ ersetzt wird
            //TODO: deaktiviert wegen NullPointerExeption


            //nextURL ist nicht von robots.txt-Blacklist betroffen
            //Ausgabe von Fehlern abschalten
            webClient.getOptions().setThrowExceptionOnScriptError(false);
            webClient.getOptions().setJavaScriptEnabled(false);
            webClient.getOptions().setCssEnabled(false);
            try {
                System.out.println("Load URL: " + nextURL);
                System.out.println("Hello " + nextURL + "\nWelcome to AFFE!");
                //Webseite laden
                HtmlPage page = webClient.getPage(nextURL);

                //Analyse der Webseite
                analyzePage(nextURL, page, targetId);
            } catch (Exception e) {
                System.out.println(e.getMessage());
                DataBaseFunction.updateTargetNextVisit(targetId, "Exception", "");
            } catch (Error err) {
                err.printStackTrace();
                DataBaseFunction.updateTargetNextVisit(targetId, err.getClass().getSimpleName(), "");
            } finally {
                //Webclient vorbereiten für nächste Webseite
                webClient.getCache().clear();
                webClient.close();
                webClient = new WebClient();
            }
            //TODO: 02 löschen
//           catch (UnknownHostException uhe) {
//                System.out.println(uhe.getMessage());
//                updateTargetNextVisit(targetId, "UnknownHostException", "");
//            } catch (IllegalArgumentException iae) {
//                System.out.println(iae.getMessage());
//                updateTargetNextVisit(targetId, "IllegalArgumentException", "");
//            }catch (NullPointerException npe){
//                System.out.println(npe.getMessage());
//                updateTargetNextVisit(targetId,"NullPointerException", "");
//            } catch (ClassCastException cce) {
//                System.out.println(cce.getMessage());
//                updateTargetNextVisit(targetId, "ClassCastException", "");
//            } catch (FailingHttpStatusCodeException fhsce) {
//                System.out.println(fhsce.getMessage());
//                updateTargetNextVisit(targetId, "FailingHttpStatusCodeException", "");
//            }


            //Programmdurchlauf stoppen
            if (countReadPages >= durchlaeufe) {
                stop = true;
            } else {
                ++countReadPages;

                //crawlerdelay berücksichtigen bei gleichem Host
                //TODO: robot npe: deaktiviert
                if (getHostUrl(nextURL).equals(lastURL)) {
                    try {
                        TimeUnit.SECONDS.sleep(currentRobot.getDelay());
                    } catch (IllegalArgumentException iae) {
                        System.out.println("Crawlerdelay ist unzulässig.");
                    } catch (InterruptedException ie) {
                        System.out.println("InterruptedException thrown in Main.");
                    }
                }
                //aktuellen Host für nächsten Durchlauf ablegen
                //TODO: robot npe: deaktiviert
                lastURL = getHostUrl(nextURL);
            }
        }
    }

    public static int getTargetId(String currentURL) {
        return DataBaseFunction.readTargetId(currentURL);
    }

    public static void analyzePage(String currentURL, HtmlPage page, int targetId) {
        HashMap<String, Integer> keywords = new HashMap<>();
        System.out.println("Website Language: " + page.getDocumentElement().getAttribute("lang"));
        String seitensprache = page.getDocumentElement().getAttribute("lang");
        seitensprache = seitensprache.trim();
        String sprache = seitensprache.toLowerCase();
//        sprache = seitensprache.toLowerCase();
//        sprache = seitensprache.toLowerCase();
        System.out.println("Seitensprache : " + seitensprache);
//        System.out.println("Sprache : " + sprache);


        if ((sprache.matches("en.*"))) {
            FileReader.readBlacklistKeyword("C:\\Users\\DCV\\Desktop\\webcrawlerEnglisch.txt");
            System.out.println("Englisch erkannt");
        } else if ((sprache.matches("de.*"))) {
            FileReader.readBlacklistKeyword("C:\\Users\\DCV\\Desktop\\webcrawlerDeutsch.txt");
            System.out.println("deutsch erkannt");
        } else {
            System.out.println("Websitelanguage: Für die Sprache existiert keine Blacklist.");
        }

        UrlParser.analyzeHyperlinks(page.getBaseURL(), page.getBody());
        String description = analyzeDescription(page.getBaseURL(), page.getHead(), targetId);
        if (description == null) {
            description = createDescription(page.getBody());
        }
        String title = analyzePageTitle(targetId, page.getBaseURL(), page.getHead(), keywords);
        KeywordMetaParser.analyzeKeywordMetaTag(currentURL, page, keywords);
        KeywordHeadlineParser.analyzeKeywordHeaderTag(currentURL, page, keywords);
        clearAndRegisterKeywords(targetId, keywords);
        DataBaseFunction.updateTargetNextVisit(targetId, title, description);
    }

    /**
     * reads title of a website, extends keywordlist and returns the longest title found on website.
     * Form 1: <title> content </title>
     * Form 2: <meta name"title" content=" content ">
     *
     * @param targetId    id der currentURL in DB_target
     * @param currentURL  URL der aktuellen Webseite
     * @param htmlElement zu durchsuchende DomNode-Elemente
     * @param keywords    HashMap<String, Integer> Liste aller Keywords in currentURL
     * @return title als String
     */
    public static String analyzePageTitle(int targetId, URL currentURL, DomNode htmlElement, HashMap<String, Integer> keywords) {
        String title = "";
        String title1 = "";
        String title2 = "";

        for (DomNode d : htmlElement.getChildren()) {
            if (d.getLocalName() != null) {
                //Form 1
                if (d.getLocalName().equals("title")) {
                    //Konsolenausgabe: Inhalt des HtmlTag
//                    System.out.println("Title: " + d.getTextContent());
                    title1 = d.getTextContent();
                    //Titel in Keywörter aufteilen
                    registerKeywords(d.getTextContent(), 5, keywords);
                }

                //Form 2
                if (d.getLocalName().equals("meta")) {
                    //---für die Anzahl seiner Parameter/Attributes
                    for (int i = 0; i < d.getAttributes().getLength(); i++) {
                        Node n = d.getAttributes().item(i);
                        if ((n.getNodeName().equals("name")) && n.getNodeValue().equals("title")) {
                            for (int j = 0; j < d.getAttributes().getLength(); j++) {
                                Node m = d.getAttributes().item(i++);
                                if (m.getNodeName().equals("content")) {
                                    //Konsolenausgabe: Inhalt des HtmlTag
//                                    System.out.println("contentNode: " + m.getNodeName());
//                                    System.out.println("wert: " + m.getNodeValue());
                                    title2 = m.getNodeValue();
                                    registerKeywords(title, 5, keywords);
                                }
                            }
                        }
                    }
                }
            }
        }
        if (title1.length() > title2.length()) {
            title = title1;
        } else {
            title = title2;
        }
        return title;
    }

    /**
     * searches htmlElement for a description and returns the longest description found.
     *
     * @param currentURL  URL of currently scanned website
     * @param htmlElement DomNode which gets scanned for description
     * @param targetId    id of URL in DB_target
     * @return description as String
     */
    public static String analyzeDescription(URL currentURL, DomNode htmlElement, int targetId) {
        String descriptionText = "";
        String description1 = "";
        String description2 = "";

        for (DomNode d : htmlElement.getChildren()) {
            if (d.getLocalName() != null) {
                //Form 1
                //description aus <meta name="description" content="beschreibung der seite">
                if (d.getLocalName().equals("meta")) {
                    //---für die Anzahl seiner Parameter/Attributes
                    for (int i = 0; i < d.getAttributes().getLength(); i++) {
                        Node n = d.getAttributes().item(i);
                        if ((n.getNodeName().equals("name")) && n.getNodeValue().equals("description")) {
                            for (int j = 0; j < d.getAttributes().getLength(); j++) {
                                Node m = d.getAttributes().item(i++);
                                if (m.getNodeName().equals("content")) {
                                    //Konsolenausgabe: Inhalt des HtmlTag
//                                    System.out.println("contentNode: " + m.getNodeName());
//                                    System.out.println("wert: " + m.getNodeValue());
                                    description1 = m.getNodeValue();
                                }
                            }
                        }
                    }
                }
                //Form 2
                //description aus <description="">
                if (d.getLocalName().equals("description")) {
                    //Konsolenausgabe: Inhalt des HtmlTag
//                    System.out.println("Beschreibung: " + d.getTextContent());
                    description2 = d.getTextContent();
                }
            }
        }
        //Auswahl der längsten description zum ablegen in DB
        if (description1.length() > description2.length()) {
            descriptionText = description1;
        } else {
            descriptionText = description2;
        }
        //keine description auf Seite vorhanden
        if (descriptionText.length() < 1) {
//            System.out.println("Keine Beschreibung gefunden auf " + currentURL + ".");
            descriptionText = null;
        }
        return descriptionText;
    }

    /**
     * clears old keywords of targetId = FK_targetId at DB-searchresult and writes all keywords from keywordlist into
     * DB.searchresult
     *
     * @param targetId id of URL in DB.target
     * @param keywords list of keywords found on currently analyzed website
     */
    public static void clearAndRegisterKeywords(int targetId, HashMap<String, Integer> keywords) {
        DataBaseFunction.clearKeywords(targetId);
        for (String k : keywords.keySet()) {
            //--keyword, relevanz, targetId an SQL übergeben
            DataBaseFunction.writeKeyword(k, keywords.get(k), targetId);
        }
    }

    /**
     * formats and checks whether a word is to be registered as a keyword. Duplicate entrys are filtered out.
     *
     * @param textForKeywords String to be checked
     * @param relevanz        given importance of the keyword-source
     * @param keywords        list of keywords found for that website
     */
    public static void registerKeywords(String textForKeywords, int relevanz, HashMap<String, Integer> keywords) {
        //Blacklist Bindewörter
        //TODO: Seitensprache ermitteln und entsprechende Blacklist wählen
        /*englisch = C:\Users\DCV\Desktop\webcrawlerEnglisch.txt
                duetsch = C:\Users\DCV\Desktop\webcrawlerDeutsch.txt
                polnisch = C:\Users\DCV\Desktop\webcrawlerPolnisch.txt
         String seitensprache  */
        //TODO: Seitensprache ermitteln
        //TODO: Blacklist aus externem Log/Dokument entsprechend Seitensprache
        //TODO: verschiedenen Blacklist-Pfade als Konstante hinterlegen für jede Sprache
        //       List<String> disabledKeywords = FileReader.readBlacklistKeyword("C:\\Users\\DCV\\Desktop\\webcrawlerDeutsch.txt");
        //?
        List<String> disabledKeywords = null;
        try {
            disabledKeywords = FileReader.multipleFileReader("webcrawlerBlacklist.*txt");
        } catch (Exception e) {
            e.printStackTrace();
        }


        //alles was kein Schriftzeichen ist entfernen, ö.ä,ü bleiben bestehen
        textForKeywords = textForKeywords.replaceAll("[\\p{Punct}]+", " ")
                .replaceAll("[\\p{Digit}]+", " ") //alle Zahlen ...
                .replaceAll("\n", " ")            //linebreak entfernen
                .replaceAll("[ ]+", " ");
        String[] newKeywords = textForKeywords.split(" ");
        for (String word : newKeywords) {
            word = word.toLowerCase().trim();
            if ((word.length() > 2) && (!disabledKeywords.contains(word))) {
                if ((!keywords.containsKey(word)) || (relevanz > keywords.get(word))) {
//                    System.out.println("registerKeywords(" + word + ", " + relevanz + ")");
                    keywords.put(word, relevanz);
                }
            }
        }
    }

    public static void printDomTree(String prefix, DomNode htmlElement) {
        System.out.println(prefix + htmlElement.getNodeName());
        for (DomNode d : htmlElement.getChildren()) {
            printDomTree(prefix + "  ", d);
        }
    }

    /**
     * lookup a href and h2 in htmlElement, prints them into console if found (rekursiv)
     * TODO: test if style elements in HTML-Tag are recognized
     *
     * @param htmlElement contains all Elements from a specific part of a html website
     * @param baseUrl     is the URL where the htmlElements are from
     */
    public static void printResultHeader(DomNode htmlElement, URL baseUrl) {
        //alle h2 überschriften auslesen
        if (htmlElement.getNodeName().equals("h2")) {
            System.out.println("h2:" + htmlElement.getTextContent());
        }

        //alle Links auslesen
        //-LinkTag finden
        if (htmlElement.getNodeName().equals("a")) {
            for (int i = 0; i < htmlElement.getAttributes().getLength(); i++) {
                Node n = htmlElement.getAttributes().item(i);
                //--Link finden
                if (n.getNodeName().equals("href")) {
                    try {
                        System.out.println("a: " + baseUrl.toURI().resolve(n.getNodeValue()));
                        //DB auf vorhandene URL prüfen
//                        if (DataBaseFunction.newTarget(baseUrl.toURI().resolve(n.getNodeValue()).toString())) {
//                            //DB row schreiben wenn URL nicht vorhanden
//                            DataBaseFunction.writeTargetToTargetlist(baseUrl.toURI().resolve(n.getNodeValue()).toString());
//                        }
                    } catch (URISyntaxException use) {
                        System.out.println(use.getMessage());
                    }
                }
            }
        }

        for (DomNode d : htmlElement.getChildren()) {
            printResultHeader(d, baseUrl);
        }
    }

    /**
     * lists information about memory of JAVA-VirtualMachine
     * TODO: prüfen ob es mit Bytes oder KBytes arbeitet -> mb = 1024 * 1024 * 1024
     */
    public static void printMemory() {
        Runtime runtime = Runtime.getRuntime();
        NumberFormat format = NumberFormat.getInstance();
        StringBuilder sb = new StringBuilder();
        long maxMemory = runtime.maxMemory();
        long allocatedMemory = runtime.totalMemory();
        long freeMemory = runtime.freeMemory();

        long mb = 1024 * 1024;
        sb.append("Used memory: " + ((runtime.totalMemory() - runtime.freeMemory()) / (float) mb) + " Megabyte");
//        sb.append("free memory: " + format.format(freeMemory / 1024) + "\n");
//        sb.append("allocated memory: " + format.format(allocatedMemory / 1024) + "\n");i
//        sb.append("max memory: " + format.format(maxMemory / 1024) + "\n");
//        sb.append("total free memory: " + format.format((freeMemory + (maxMemory - allocatedMemory)) / 1024) + "\n");
        System.out.println(sb);
    }

    /**
     * looks up content of the first <p></p> Element and returns it
     *
     * @param htmlElement page from the current URL
     * @return alternativ description or null if no p is found
     */
    public static String createDescription(DomNode htmlElement) {

        for (DomNode d : htmlElement.getChildren()) {
            if (d.getLocalName() != null) {
                //Element welches wir suchen
                if (d.getLocalName().equals("p") || d.getLocalName().equals("p%")) {
                    return d.getTextContent();
                }
            }
            String description = createDescription(d);
            if (description != null) {
                return description;
            }
        }
//        return null;
        return "Für diese Seite ist keine Beschreibung verfügbar.";
    }

    public static String analyzeRobotsTxt(URL currentURL, DomNode htmlElement, int targetId) {
        String RobotsText = "";
        String RobotsText1 = "";
        String RobotsText2 = "";

        for (DomNode d : htmlElement.getChildren()) {
            if (d.getLocalName() != null) {
                //Form 1
                //description aus <meta name="description" content="beschreibung der seite">
                if (d.getLocalName().equals("meta")) {
                    //---für die Anzahl seiner Parameter/Attributes
                    for (int i = 0; i < d.getAttributes().getLength(); i++) {
                        Node n = d.getAttributes().item(i);
                        if ((n.getNodeName().equals("name")) && n.getNodeValue().equals("RobotsText")) {
                            for (int j = 0; j < d.getAttributes().getLength(); j++) {
                                Node m = d.getAttributes().item(i++);
                                if (m.getNodeName().equals("content")) {
                                    //Konsolenausgabe: Inhalt des HtmlTag
//                                    System.out.println("contentNode: " + m.getNodeName());
//                                    System.out.println("wert: " + m.getNodeValue());
                                    RobotsText1 = m.getNodeValue();
                                }
                            }
                        }
                    }
                }
                //Form 2
                if (d.getLocalName().equals("RobotsText")) {
                    //Konsolenausgabe: Inhalt des HtmlTag
//                    System.out.println("Beschreibung: " + d.getTextContent());
                    RobotsText2 = d.getTextContent();
                }
            }
        }
        if (RobotsText1.length() > RobotsText2.length()) {
            RobotsText = RobotsText1;
        } else {
            RobotsText = RobotsText2;
        }
        if (RobotsText.length() < 1) {
            System.out.println("Keine Beschreibung gefunden auf " + currentURL + ".");
            RobotsText = "Fehler: Seite prüfen.";
            //TODO: generate RobotsText in case none is found
        }

        return "RobotsText";
    }

    /**
     * Takes a Url in String-format and cuts away everything that isnt part of the HostUrl.
     *
     * @param nextURL Url as a String (ie.: http://www.mainurl.com/login/problem)
     * @return the Host Url (ie.: http://www.mainurl.com)
     */
    public static String getHostUrl(String nextURL) {
        URL hostUrl = null;
        //-hostUrl
        String base = "";
        //-hostURL ermitteln
        try {
            hostUrl = new URL(nextURL);
            base = hostUrl.getProtocol() + "://" + hostUrl.getHost();
        } catch (MalformedURLException mue) {
            System.out.println("Main.getHostUrl : URL hatte kein zulässiges Format!");
        }
//        System.out.println("Main.hetHostUrl = " + base);
        return base;
    }
}
