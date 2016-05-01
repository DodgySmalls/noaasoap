// 
// Make a query using SOAP to the NOAA Sea Level data base.
// This code is an extension of NOAA sample client code: http://opendap.co-ops.nos.noaa.gov/axis/webservices/waterlevelverifiedmonthly/samples/client.html
// The sample code contained potential issues which are discussed in the repo file "SOAPRequestExamples.txt"


package NOAAsoap;

import javax.xml.soap.*;
import java.util.Iterator;
import java.net.URL;
import java.io.*;
import java.util.Arrays;
import java.util.List;
import java.util.ArrayList;
import java.util.EnumSet;

public class NOAAQuery {
    public static final String DEFAULT_DATUM = NOAAXML.DATUM_MSL;
    public static final String DEFAULT_DATE_BEGIN = "19500101 00:00";
    public static final String DEFAULT_IN_FILE = "stationlist.dat";
    public static final String DEFAULT_OUT_PATH = "./";

    public static final String ARG_DEBUG = "-debug";
    public static final String ARG_VERBOSE = "-verbose";
    public static final String ARG_SUPPRESS_ERROR_CONSOLE = "-suppress-errors";
    public static final String ARG_SUPPRESS_CONSOLE = "-suppress-console";

    public static final String ARG_RAW = "-raw";
    public static final String ARG_RAW_ONLY = "-raw-only";
    public static final String ARG_SUPPRESS_FILES = "-suppress-files";
    
    public static final String ARG_REQUEST_DATUM = "-datum";
    public static final String ARG_REQUEST_STARTDATE = "-from";
    public static final String ARG_REQUEST_ENDDATE = "-to";
    public static final String ARG_REQUEST_MONTHS = "-months";

    public static final String ARG_INP_FORMAT = "-inpf";
    public static final String ARG_OUT_FORMAT = "-outf";

    public static final String ARG_CSV = "CSV";
    public static final String ARG_XML = "XML";

    private enum Flag {
        NIL, ENABLE_RAW_OUTPUT, ENABLE_FILE_OUTPUT, ENABLE_CONSOLE, ENABLE_VERBOSE_CONSOLE, ENABLE_DEBUG_CONSOLE, ENABLE_ERROR_CONSOLE
    }
    private static EnumSet<Flag> globalFlags = EnumSet.of(Flag.NIL);
    private static FileDescription inFileDesc = new FileDescription();
    private static FileDescription outFileDesc = new FileDescription();
    private static Request request = new Request();

    public static void main(String[] args) {
        List<String> stations = new ArrayList<String>();
        
        if(parseArguments(parseFlags(new CaseInsensitiveList(Arrays.asList(args)))).size() > 0){
            printDebug("Excess arguments");
        }

        defaults(); //values which weren't specified are loaded

        printDebug(inFileDesc.toString());
        printDebug(outFileDesc.toString());
        printDebug(request.toString());
        if(globalFlags.contains(Flag.ENABLE_DEBUG_CONSOLE)) {
            System.out.print("[DEBUG]");
            if(globalFlags.contains(Flag.ENABLE_CONSOLE)) {
                System.out.print("ENABLE_CONSOLE");
            } 
            if(globalFlags.contains(Flag.ENABLE_VERBOSE_CONSOLE)) {
                System.out.print("|ENABLE_VERBOSE_CONSOLE");
            }
            if(globalFlags.contains(Flag.ENABLE_DEBUG_CONSOLE)) {
                System.out.print("|ENABLE_DEBUG_CONSOLE");
            }
            if(globalFlags.contains(Flag.ENABLE_ERROR_CONSOLE)) {
                System.out.print("|ENABLE_ERROR_CONSOLE");
            }
            if(globalFlags.contains(Flag.ENABLE_FILE_OUTPUT)) {
                System.out.print("|ENABLE_FILE_OUTPUT");
            }
            if(globalFlags.contains(Flag.ENABLE_RAW_OUTPUT)) {
                System.out.print("|ENABLE_RAW_OUTPUT");
            }
            System.out.print("\n");
        }

        //Verify/Load input file
        try {
            inFileDesc.setFileName( (inFileDesc.getFileName() == null) ? DEFAULT_IN_FILE : inFileDesc.getFileName() );
            stations = loadStationsFromFile(inFileDesc);
        } catch (Exception e) {
            printError("[ERROR] Could not load file \"" + inFileDesc.getFileName() +"\"");
            return;
        }

        //Verify output path
        //If the definition of DEFAULT_OUT_PATH is changed (anything other than "./"), that directory will need to be verified as well
        outFileDesc.setDirectoryName( (outFileDesc.getDirectoryName() == null) ? DEFAULT_OUT_PATH : outFileDesc.getDirectoryName() );
        if(!DEFAULT_OUT_PATH.equals(outFileDesc.getDirectoryName())) {
            File file = new File(outFileDesc.getDirectoryName());
            if(file.exists()) {
                if(file.isFile() || !file.isDirectory()) {
                    printError("[ERROR] Requested output path is an existing file (not a directory).");
                    return;
                }
            } else {
                file.mkdirs();
                if(!file.isDirectory()) {
                    printError("[ERROR] Error while attempting to create requested output directories.");
                    return;
                }
            }
            try {
                if(outFileDesc.getDirectoryName().charAt(outFileDesc.getDirectoryName().length() - 1) != '/') {
                    outFileDesc.setDirectoryName(outFileDesc.getDirectoryName() + "/");
                }
            } catch (Exception e) {
                printError("[ERROR] Error while parsing the given output directory name.");
            }
        }

        try {
            SOAPConnectionFactory soapConnectionFactory = SOAPConnectionFactory.newInstance();
            SOAPConnection connection = soapConnectionFactory.createConnection();
            URL endpoint = new URL("http://opendap.co-ops.nos.noaa.gov/axis/services/WaterLevelVerifiedMonthly");
            for(String id : stations) {
                try {
                    print("Querying station: " + id);
                    request.setStation(id);
                    SOAPMessage message = prepareMessage(request);
                    SOAPMessage response = connection.call(message, endpoint);
                    outFileDesc.setFileName(id);
                    handleResponse(response, outFileDesc);
                } catch (SOAPException e) {
                    printError("[ERRROR] " + e.toString());
                }
            }
            connection.close();
        } catch (SOAPException e) {
            printError("[ERRROR] " + e.toString());
        } catch (IOException io) {
            printError("[ERROR] " + io.toString());
        }
    }

    private static void handleResponse(SOAPMessage response, FileDescription desc) {
        try{
            SOAPBody responseBody = response.getSOAPBody();
            
            //Raw passthrough
            if (globalFlags.contains(Flag.ENABLE_RAW_OUTPUT)) {
                try {
                    FileOutputStream rawOut = new FileOutputStream (desc.getDirectoryName() + desc.getFileName() + "_response.xml");
                    response.writeTo(rawOut);
                    rawOut.close();
                } catch (IOException e) {
                    printError("[ERROR] Exception occurred while attempting to write raw output.\n" + e.toString());
                }
            }

            desc.addExtensionToFileName();

            if (responseBody.hasFault()) {
                SOAPFault fault = responseBody.getFault();
                String actor = fault.getFaultActor();
                //System.out.println("Fault contains: ");
                //System.out.println("Fault code: " + fault.getFaultCodeAsName().getQualifiedName());
                System.out.println("Fault      : " + fault.getFaultString());
                if (actor != null) {
                    System.out.println("Actor      : " + actor);
                }
         
            } else {
                Iterator iterator = responseBody.getChildElements();
                Iterator iterator2 = null;
                Iterator iterator3 = null;
               
                String tagName = null;
                SOAPElement se = null;

                FileOutputStream fOut;

                if(globalFlags.contains(Flag.ENABLE_FILE_OUTPUT)) {
                    fOut = new FileOutputStream(desc.getDirectoryName() + desc.getFileName());
                    if (iterator.hasNext()) {
                        se = (SOAPElement) iterator.next();
                        iterator = se.getChildElements();
                        while (iterator.hasNext()) {
                            se = (SOAPElement) iterator.next();
                            tagName = se.getElementName().getLocalName();
                            if (NOAAXML.NODE_DATA.equals(tagName)) {
                                iterator2 = se.getChildElements();
                                while (iterator2.hasNext()) {
                                    se = (SOAPElement) iterator2.next();
                                    tagName = se.getElementName().getLocalName();
                                    if(NOAAXML.NODE_ITEM.equals(tagName) && globalFlags.contains(Flag.ENABLE_FILE_OUTPUT)) {
                                        writeItemNode(se, fOut, outFileDesc.getFormat(), request.getDatum());
                                    }
                                }
                            }
                        }
                    }
                    fOut.close();
                }

                if(globalFlags.contains(Flag.ENABLE_VERBOSE_CONSOLE)) {
                    iterator = responseBody.getChildElements();
                    if (iterator.hasNext()) {
                        se = (SOAPElement) iterator.next();
                        iterator = se.getChildElements();
                        while (iterator.hasNext()) {
                            se = (SOAPElement) iterator.next();
                            printMetadata(se);
                            tagName = se.getElementName().getLocalName();
                            if (NOAAXML.NODE_DATA.equals(tagName)) {
                                iterator2 = se.getChildElements();
                                while (iterator2.hasNext()) {
                                    se = (SOAPElement) iterator2.next();
                                    tagName = se.getElementName().getLocalName();
                                    iterator3 = se.getChildElements();
                                    while (iterator3.hasNext()) {
                                        se = (SOAPElement) iterator3.next();
                                        if(globalFlags.contains(Flag.ENABLE_VERBOSE_CONSOLE)) {
                                            printData(se);
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }catch(Exception e) {
            printError("[ERROR] " + e.toString());
        }
    }

    public static SOAPMessage prepareMessage(Request r) {
        SOAPMessage message = null;
        try {
            SOAPFactory soapFactory = SOAPFactory.newInstance();

            MessageFactory factory = MessageFactory.newInstance();
            message = factory.createMessage();
            SOAPPart part = message.getSOAPPart();
            SOAPEnvelope envelope = part.getEnvelope();
            envelope.addNamespaceDeclaration("water", "http://opendap.co-ops.nos.noaa.gov/");

            MimeHeaders headers = message.getMimeHeaders();
            headers.addHeader("SOAPAction", "http://opendap.co-ops.nos.noaa.gov/axis/webservices/waterlevelverifiedmonthly/wsdl");


            SOAPBody body = envelope.getBody();
            Name bodyName = soapFactory.createName("getWLVerifiedMonthlyAndMetadata", "water", 
                                "http://opendap.co-ops.nos.noaa.gov/axis/webservices/waterlevelverifiedmonthly/wsdl");
            SOAPBodyElement bodyElement = body.addBodyElement(bodyName);

            //Constructing the body for the request
            Name name = soapFactory.createName(NOAAXML.ELEM_STATION_ID);
            SOAPElement symbol = bodyElement.addChildElement(name);
            symbol.addTextNode(r.getStation());
            name = soapFactory.createName(NOAAXML.ELEM_DATE_BEGIN);
            symbol = bodyElement.addChildElement(name);
            symbol.addTextNode(r.getStart());
            name = soapFactory.createName(NOAAXML.ELEM_DATE_END);
            symbol = bodyElement.addChildElement(name);
            symbol.addTextNode(r.getEnd());
            name = soapFactory.createName(NOAAXML.ELEM_DATUM);
            symbol = bodyElement.addChildElement(name);
            symbol.addTextNode(r.getDatum());
            name = soapFactory.createName(NOAAXML.ELEM_UNIT);
            symbol = bodyElement.addChildElement(name);
            symbol.addTextNode("0");
            name = soapFactory.createName(NOAAXML.ELEM_TIME_ZONE);
            symbol = bodyElement.addChildElement(name);
            symbol.addTextNode("0"); //do conversions on our end, where we know NOAA database timezone is GMT
        } catch (SOAPException e) {
            printError("[ERROR] " + e.toString());
        }
        return message;
    }

    /* Reads an input file (CSV or XML) and returns a list of NOAA station ids from that input file
    |
    |*/
    public static List<String> loadStationsFromFile(FileDescription fd) throws Exception {
        ArrayList<String> ids = new ArrayList<String>();
        File inFile = new File(fd.getFileName());
        if(!inFile.exists()) {
            throw new Exception();
        }
        if(!inFile.isFile()) {
            throw new Exception();
        }

        //default format is comma separated values
        if(fd.getFormat() == null || fd.getFormat() == FileDescription.Format.CSV) {
            FileReader inFileReader = new FileReader(fd.getFileName());
            BufferedReader bufferedReader = new BufferedReader(inFileReader);

            String inLine = "";
            
            while((inLine = bufferedReader.readLine()) != null) {
                inLine.replaceAll("\\s+", "");           //remove all whitespace
                String[] subStrings = inLine.split(","); //split on comma
                if(subStrings[0] != null) {
                    try {
                        Integer.parseInt(subStrings[0]); //ensure that the number is some valid integer
                        ids.add(subStrings[0]);          //first value on each row is NOAA database station id
                    } catch (Exception e) {
                        printDebug("Input list contained a malformed line");
                    }   
                }    
            }
        } else if (fd.getFormat() == FileDescription.Format.XML) {
            //TODO XML input file implementation
        }

        if(globalFlags != null) {
            if(globalFlags.contains(Flag.ENABLE_DEBUG_CONSOLE)) {
                System.out.print("\nList of station ids:\n");
                for(String s : ids) {
                    System.out.print(s + ",");
                }
                System.out.print("\n");
            }
        }

        return ids;
    }

    /* Writes the requested values found in the given SOAPElement as long as it conforms to the following structure:
    |  se => <item>
    |            <datum_name> value </datum_name>
    |            <datum_name> value </datum_name>
    |            ...
    |            <datum_name> value </datum_name>
    |        </item>
    | 
    | CSV output format:
    |     YYYY/MM/DD HH:MM, datum_value
    |     YYYY/MM/DD HH:MM, datum_value
    |     ...
    | 
    | XML output format:
    |     <data>
    |         <date> date <date>
    |         <datum_name> value </datum_name>
    |     </data>
    |     <data> 
    |         <datum_name> date <date>
    |         <datum_name> value </datum_name>
    |     </data>
    |     ...
    |*/
    public static void writeItemNode(SOAPElement se, FileOutputStream fOut, FileDescription.Format format, String datum) { 
        Iterator iterator;
        String tagName, timeStamp, csValues, finalOutput;
        SOAPElement elem = se;

        iterator = elem.getChildElements();
        if(format == FileDescription.Format.CSV) {
            try {
                csValues = "";
                timeStamp = "missing_timestamp"; //TODO verify procedure for malformed responses
                while (iterator.hasNext()) {
                    elem = (SOAPElement) iterator.next();
                    tagName = elem.getElementName().getLocalName();
                    if (tagName != null && NOAAXML.ELEM_TIME_STAMP.equals(tagName)) {
                        timeStamp = elem.getValue();
                    } else if (tagName != null && datum.equals(tagName)) {
                        csValues += ", " + elem.getValue();
                    }
                }
                finalOutput = timeStamp + csValues + "\n";
                try {
                    fOut.write(finalOutput.getBytes());
                }catch (IOException e) {
                    printError("[ERROR] Failure when writing data to file.");
                }
            } catch (Exception e) {
                printDebug("writeItemNode() was called on a bad node, which resulted in a traversal exception.");
            }
        } else if(format == FileDescription.Format.XML) {

            //TODO XML output
        }
    }
    

    /* If the given SOAPElement is a metadata element, prints/formats it to console
    |
    |*/
    public static void printMetadata(SOAPElement se) {
        String tagName = se.getElementName().getLocalName();
        if (tagName != null) {
            if (NOAAXML.ELEM_STATION_ID.equals(tagName)) {
                System.out.println("Printing Metadata \n");
                System.out.println("Station ID       : " + se.getValue());
            } else if (NOAAXML.ELEM_STATION_NAME.equals(tagName)) {
                System.out.println("Station Name     : " + se.getValue());
            } else if (NOAAXML.ELEM_LATITUDE.equals(tagName)) {
                System.out.println("Latitude         : " + se.getValue());
            } else if (NOAAXML.ELEM_LONGITUDE.equals(tagName)) {
                System.out.println("Longitude        : " + se.getValue());
            } else if (NOAAXML.ELEM_STATE.equals(tagName)) {
                System.out.println("State            : " + se.getValue());
            } else if (NOAAXML.ELEM_DATA_SOURCE.equals(tagName)) {
                System.out.println("Data Source      : " + se.getValue());
            } else if (NOAAXML.ELEM_DATE_BEGIN.equals(tagName)) {
                System.out.println("Begin Date       : " + se.getValue());
            } else if (NOAAXML.ELEM_DATE_END.equals(tagName)) {
                System.out.println("End Date         : " + se.getValue());
            } else if (NOAAXML.ELEM_DATUM.equals(tagName)) {
                System.out.println("Datum            : " + se.getValue());
            } else if (NOAAXML.ELEM_UNIT.equals(tagName)) {
                System.out.println("Unit             : " + se.getValue());
            } else if (NOAAXML.ELEM_TIME_ZONE.equals(tagName)) {
                System.out.println("Time Zone        : " + se.getValue() + "\n");
                System.out.println("Printing the data \n");
            }
        }
    }

    /* If the given SOAPElement is an observation element (datum with a value), prints/formats it to console
    |
    |*/
    public static void printData(SOAPElement se) {
        String tagName = se.getElementName().getLocalName();
        if (tagName != null) {
            if (NOAAXML.ELEM_TIME_STAMP.equals(tagName)) {
                System.out.println("Time Stamp: " + se.getValue());
            } else if (NOAAXML.DATUM_MHHW.equals(tagName)) {
                System.out.println("MHHW      : " + se.getValue());
            } else if (NOAAXML.DATUM_MHW.equals(tagName)) {
                System.out.println("MHW       : " + se.getValue());
            } else if (NOAAXML.DATUM_DTL.equals(tagName)) {
                System.out.println("DTL       : " + se.getValue());
            } else if (NOAAXML.DATUM_MTL.equals(tagName)) {
                System.out.println("MTL       : " + se.getValue());
            } else if (NOAAXML.DATUM_MSL.equals(tagName)) {
                System.out.println("MSL       : " + se.getValue());
            } else if (NOAAXML.DATUM_MLW.equals(tagName)) {
                System.out.println("MLW       : " + se.getValue());
            } else if (NOAAXML.DATUM_MLLW.equals(tagName)) {
                System.out.println("MLLW      : " + se.getValue());
            } else if (NOAAXML.DATUM_GT.equals(tagName)) {
                System.out.println("GT        : " + se.getValue());
            } else if (NOAAXML.DATUM_MN.equals(tagName)) {
                System.out.println("MN        : " + se.getValue());
            } else if (NOAAXML.DATUM_DHQ.equals(tagName)) {
                System.out.println("DHQ       : " + se.getValue());
            } else if (NOAAXML.DATUM_DLQ.equals(tagName)) {
                System.out.println("DLQ       : " + se.getValue());
            } else if (NOAAXML.DATUM_HWI.equals(tagName)) {
                System.out.println("HWI       : " + se.getValue());
            } else if (NOAAXML.DATUM_LWI.equals(tagName)) {
                System.out.println("LWI       : " + se.getValue());
            } else if (NOAAXML.ELEM_MAX.equals(tagName)) {
                System.out.println("Highest Tide  : " + se.getValue());
            } else if (NOAAXML.ELEM_MIN.equals(tagName)) {
                System.out.println("Lowest Tide   : " + se.getValue());
            } else if (NOAAXML.ELEM_INFERENCE.equals(tagName)) {
                System.out.println("Inferred Tide : " + se.getValue());
            }
        }
    }

    private static CaseInsensitiveList parseFlags(CaseInsensitiveList arguments) {
        //By default the regular and error consoles are enabled, and file output is enabled.
        globalFlags.add(Flag.ENABLE_CONSOLE);
        globalFlags.add(Flag.ENABLE_ERROR_CONSOLE);
        globalFlags.add(Flag.ENABLE_FILE_OUTPUT);

        //Consoles
        if(arguments.contains(ARG_DEBUG)) {
            globalFlags.add(Flag.ENABLE_DEBUG_CONSOLE);
            System.out.print("\nDEBUG ENABLED\nargs:");
            for(String s : arguments) {
                System.out.print("(" + s + ") ");
            }
            System.out.print("\n");
            arguments.remove(ARG_DEBUG);
        }
        if(arguments.contains(ARG_VERBOSE)) {
            globalFlags.add(Flag.ENABLE_VERBOSE_CONSOLE);
            arguments.remove(ARG_VERBOSE);
        }
        if(arguments.contains(ARG_SUPPRESS_CONSOLE)) {
            globalFlags.remove(Flag.ENABLE_CONSOLE);
            globalFlags.remove(Flag.ENABLE_VERBOSE_CONSOLE);
            globalFlags.remove(Flag.ENABLE_DEBUG_CONSOLE);
            arguments.remove(ARG_SUPPRESS_CONSOLE);
        }
        if(arguments.contains(ARG_SUPPRESS_ERROR_CONSOLE)) {
            globalFlags.remove(Flag.ENABLE_ERROR_CONSOLE);
            arguments.remove(ARG_SUPPRESS_ERROR_CONSOLE);
        }

        //Files
        if(arguments.contains(ARG_RAW)) {
            globalFlags.add(Flag.ENABLE_RAW_OUTPUT);
            arguments.remove(ARG_RAW);
        }
        if(arguments.contains(ARG_RAW_ONLY)) {
            globalFlags.add(Flag.ENABLE_RAW_OUTPUT);
            globalFlags.remove(Flag.ENABLE_FILE_OUTPUT);
            arguments.remove(ARG_RAW_ONLY);
        }
        if(arguments.contains(ARG_SUPPRESS_FILES)){
            globalFlags.remove(Flag.ENABLE_RAW_OUTPUT);
            globalFlags.remove(Flag.ENABLE_FILE_OUTPUT);
            arguments.remove(ARG_SUPPRESS_FILES);
        }
        
        if(arguments.contains(ARG_REQUEST_DATUM)) {
            int index = arguments.indexOf(ARG_REQUEST_DATUM);
            try {
                String datumStr = arguments.get(index + 1);
                if(datumStr == null) {
                    throw new Exception();
                }
                request.setDatum(datumStr.toUpperCase());
            } catch(Exception e) {
                printError("[ERROR] Datum was requested but no datum was supplied.");
                printError("        Expected: [ <" + ARG_REQUEST_DATUM + "> <NOAA DATUM> ]");
                printError("        See: https://tidesandcurrents.noaa.gov/datum_options.html");
                printError("        Incorrect datum specification will invalidate output.");
                System.exit(0);
            }
            arguments.remove(index);
            arguments.remove(index);
        }

        if(arguments.contains(ARG_REQUEST_STARTDATE)) {
            int index = arguments.indexOf(ARG_REQUEST_STARTDATE);
            String rawTimeStr = null;
            try {
                String timeStampStr = arguments.get(index + 1);
                if(timeStampStr == null) {
                    throw new Exception();
                }
                rawTimeStr = timeStampStr;
            } catch(Exception e) {
                printError("[ERROR] Starting date was requested but no timestamp was supplied.");
                printError("        Expected: [ <" + ARG_REQUEST_STARTDATE + "> <YYYYMMDD HH:MM> ]");
                System.exit(0);
            }
            request.setStart(NOAAXML.cleanTimeStamp(rawTimeStr));
            if(!NOAAXML.verifyTimeStamp(request.getStart())) {
                printError("[ERROR] Starting date was requested but (" + rawTimeStr + ") could not be parsed as a valid timestamp.");
                printError("        Expected: [ <" + ARG_REQUEST_STARTDATE + "> <YYYYMMDD HH:MM> ]");
                System.exit(0);
            }
            arguments.remove(index);
            arguments.remove(index);
        }

        if(arguments.contains(ARG_REQUEST_ENDDATE)) {
            int index = arguments.indexOf(ARG_REQUEST_ENDDATE);
            String rawTimeStr = null;
            try {
                String timeStampStr = arguments.get(index + 1);
                if(timeStampStr == null) {
                    throw new Exception();
                }
                rawTimeStr = timeStampStr;
            } catch(Exception e) {
                printError("[ERROR] End date was requested but no timestamp was supplied.");
                printError("        Expected: [ <" + ARG_REQUEST_ENDDATE + "> <YYYYMMDD HH:MM> ]");
                System.exit(0);
            }
            request.setEnd(NOAAXML.cleanTimeStamp(rawTimeStr));
            if(!NOAAXML.verifyTimeStamp(request.getEnd())) {
                printError("[ERROR] End date was requested but (" + rawTimeStr + ") could not be parsed as a valid timestamp.");
                printError("        Expected: [ <" + ARG_REQUEST_ENDDATE + "> <YYYYMMDD HH:MM> ]");
                System.exit(0);
            }
            arguments.remove(index);
            arguments.remove(index);
        }

        if(arguments.contains(ARG_REQUEST_MONTHS)) {
            int index = arguments.indexOf(ARG_REQUEST_MONTHS);
            String monthCountStr = null;
            try {
                String rawMonthCountStr = arguments.get(index + 1);
                if(rawMonthCountStr == null) {
                    throw new Exception();
                }
                monthCountStr = rawMonthCountStr;
            } catch (Exception e) {
                printError("[ERROR] Requested past months but did no month count was supplied.");
                printError("        Expected: [ <" + ARG_REQUEST_MONTHS + "> <N> ]");
                System.exit(0);
            }
            int monthCount = 0;
            try {
                monthCount = Integer.parseInt(monthCountStr);
                if(monthCount < 1) {
                    throw new Exception();
                }
            } catch (Exception e) {
                printError("[ERROR] Requested past months but (" + monthCountStr + ") could not be parsed to a natural number (1, 2, 3, ...)");
                printError("        Expected: [ <" + ARG_REQUEST_MONTHS + "> <N> ]");
                System.exit(0);
            }

            request.setStart(NOAAXML.nMonthsAgoToString(NOAAXML.DATABASE_TIMEZONE, monthCount));
            arguments.remove(index);
            arguments.remove(index);

            //TODO finish previous months option
        }

        if(arguments.contains(ARG_INP_FORMAT)) {
            int index = arguments.indexOf(ARG_INP_FORMAT);
            try {
                String formatStr = arguments.get(index + 1);
                if(formatStr == null) {
                    throw new Exception();
                }
                if(ARG_CSV.equals(formatStr.toUpperCase())) {
                    inFileDesc.setFormat(FileDescription.Format.CSV);
                } else if(ARG_XML.equals(formatStr.toUpperCase())) {
                    inFileDesc.setFormat(FileDescription.Format.XML);
                } else {
                    printError("[ERROR] Input format was requested but (" + formatStr + ") could not be parsed as a valid format.");
                    printError("        Expected: [ <" + ARG_INP_FORMAT + "> <" + ARG_CSV + "|" + ARG_XML +"> ]");
                    System.exit(0);
                }
            } catch(Exception e) {
                printError("[ERROR] Input format was requested but not specified.");
                printError("        Expected: [ <" + ARG_INP_FORMAT + "> <" + ARG_CSV + "|" + ARG_XML +"> ]");
                System.exit(0);
            }
            arguments.remove(index);
            arguments.remove(index);
        }

        if(arguments.contains(ARG_OUT_FORMAT)) {
            int index = arguments.indexOf(ARG_OUT_FORMAT);
            try {
                String formatStr = arguments.get(index + 1);
                if(formatStr == null) {
                    throw new Exception();
                }
                if(ARG_CSV.equals(formatStr.toUpperCase())) {
                    outFileDesc.setFormat(FileDescription.Format.CSV);
                } else if(ARG_XML.equals(formatStr.toUpperCase())) {
                    outFileDesc.setFormat(FileDescription.Format.XML);
                } else {
                    printError("[ERROR] Output format was requested but (" + formatStr + ") could not be parsed as a valid format.");
                    printError("        Expected: [ <" + ARG_OUT_FORMAT + "> <" + ARG_CSV + "|" + ARG_XML +"> ]");
                    System.exit(0);
                }
            } catch(Exception e) {
                printError("[ERROR] Output format was requested but not specified.");
                printError("        Expected: [ <" + ARG_OUT_FORMAT + "> <" + ARG_CSV + "|" + ARG_XML +"> ]");
                System.exit(0);
            }
            arguments.remove(index);
            arguments.remove(index);
        }

        return arguments;
    }

    private static CaseInsensitiveList parseArguments(CaseInsensitiveList arguments) {
        if(arguments.size() > 0) {
            inFileDesc.setFileName(arguments.get(0));
            arguments.remove(0);
        }

        if(arguments.size() > 0) {
            outFileDesc.setDirectoryName(arguments.get(0));
            arguments.remove(0);
        }

        return arguments;
    }

    private static void defaults() {
        if(request.getStart() == null) {
            request.setStart(DEFAULT_DATE_BEGIN);
        }
        if(request.getEnd() == null) {
            request.setEnd(NOAAXML.currentTimeToString(NOAAXML.DATABASE_TIMEZONE));
        }
        if(request.getDatum() == null) {
            request.setDatum(DEFAULT_DATUM);
        }

        if(inFileDesc.getFileName() == null) {
            inFileDesc.setFileName(DEFAULT_IN_FILE);
        }
        if(inFileDesc.getFormat() == null) {
            inFileDesc.setFormat(FileDescription.Format.CSV);
        }
        if(outFileDesc.getDirectoryName() == null) {
            outFileDesc.setDirectoryName(DEFAULT_OUT_PATH);
        }
        if(outFileDesc.getFormat() == null) {
            outFileDesc.setFormat(FileDescription.Format.CSV);
        }
    }

    protected static void print(String message) {
        if(globalFlags.contains(Flag.ENABLE_CONSOLE)) {
            System.out.println(message);
        }
    }

    protected static void printDebug(String message) {
        if(globalFlags.contains(Flag.ENABLE_DEBUG_CONSOLE)) {
            System.out.println("[DEBUG]" + message);
        }
    }

    protected static void printVerbose(String message) {
        if(globalFlags.contains(Flag.ENABLE_VERBOSE_CONSOLE)) {
            System.out.println(message);
        }
    }

    protected static void printError(String message) {
        if(globalFlags.contains(Flag.ENABLE_ERROR_CONSOLE)) {
            System.err.println(message);
        }
    }
}