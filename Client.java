package NOAAsoap;

import javax.xml.soap.*;
import java.util.Iterator;
import java.net.URL;
import java.io.*;
import java.util.Arrays;
import java.util.List;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.*; // TODO precise date imports

import java.text.DateFormat;
import java.text.SimpleDateFormat;

public class Client {
    public static final String DEFAULT_DATUM = NOAAXML.DATUM_MSL; //Default to mean sea level
    public static final String DEFAULT_DATE_BEGIN = "20150101 00:00"; //TODO UPDATE DEFAULT TO 1960
    public static final String DEFAULT_IN_FILE = "stationlist.dat";
    public static final String DEFAULT_OUT_PATH = "./";
    public static final String ARG_VERBOSE = "-verbose";
    public static final String ARG_DEBUG = "-debug";
    public static final String ARG_SUPPRESS = "-suppress";
    public static final String ARG_RAW = "-raw";
    public static final String ARG_DATUM = "-datum";
    public static final String ARG_SPECIFY_START = "-from";
    public static final String ARG_MONTHS_SINCE = "-months";
    public static final String ARG_INP_FORMAT = "-inpf";
    public static final String ARG_OUT_FORMAT = "-outf";
    public static final String ARG_CSV = "csv";
    public static final String ARG_XML = "xml";

    private enum Flag {
        VERBOSE, RAW, SUPPRESS, DEBUG, NIL
    } 

    private enum Format {
        CSV, XML
    }

    private String mRequestDatum;
    private String mRequestStartDate;
    private String mRequestEndDate;
    private String mOutputPath;
    private EnumSet<Flag> mFlags;
    private Format mOutputFormat;

    public static void main(String[] args) {
        String out = DEFAULT_OUT_PATH; // TODO DONT DO THIS
        String in = null;
        String datum = null;
        String startDate = null;
        String endDate = null;
        Format inFormat = null;
        Format outFormat = null;
        EnumSet<Flag> flags = EnumSet.of(Flag.NIL);
        List<String> stations = null;
        
        //Parse cmdline args
        CaseInsensitiveList arguments = new CaseInsensitiveList(Arrays.asList(args));

        if(arguments.contains(ARG_DEBUG)) {
            flags.add(Flag.DEBUG);
            System.out.println("DEBUG ENABLED\nargs:");
            for(String s : arguments) {
                System.out.print("(" + s + ") ");
            }
            System.out.print("\n");
            arguments.remove(ARG_DEBUG);
        }
        if(arguments.contains(ARG_VERBOSE)) {
            flags.add(Flag.VERBOSE);
            arguments.remove(ARG_VERBOSE);
        }
        if(arguments.contains(ARG_RAW)) {
            flags.add(Flag.RAW);
            arguments.remove(ARG_RAW);
        }
        if(arguments.contains(ARG_SUPPRESS)) {
            flags.add(Flag.SUPPRESS);
            arguments.remove(ARG_SUPPRESS);
        }
        
        if(arguments.contains(ARG_DATUM)) {
            int index = arguments.indexOf(ARG_DATUM);
            try {
                String datumStr = arguments.get(index + 1);
                if(datumStr == null) {
                    throw new Exception();
                }
                datum = datumStr.toUpperCase();
            } catch(Exception e) {
                System.err.println("[ERROR] Datum was requested but no datum was supplied.");
                System.err.println("        Expected: [ <" + ARG_DATUM + "> <NOAA DATUM> ]");
                System.err.println("        See: https://tidesandcurrents.noaa.gov/datum_options.html");
                System.err.println("        Incorrect datum specification will invalidate output.");
                return;
            }
            arguments.remove(index);
            arguments.remove(index);
        }

        if(arguments.contains(ARG_SPECIFY_START)) {
            int index = arguments.indexOf(ARG_SPECIFY_START);
            String rawTimeStr;
            try {
                String timeStampStr = arguments.get(index + 1);
                if(timeStampStr == null) {
                    throw new Exception();
                }
                rawTimeStr = timeStampStr;
            } catch(Exception e) {
                System.err.println("[ERROR] Starting date was requested but no timestamp was supplied.");
                System.err.println("        Expected: [ <" + ARG_SPECIFY_START + "> <YYYYMMDD HH:MM> ]");
                return;
            }
            startDate = NOAAXML.prepTimeStamp(rawTimeStr);
            if(!NOAAXML.verifyTimeStamp(startDate)) {
                System.err.println("[ERROR] Starting date was requested but (" + rawTimeStr + ") could not be parsed as a valid timestamp");
                System.err.println("        Expected: [ <" + ARG_SPECIFY_START + "> <YYYYMMDD HH:MM> ]");
                return;
            }
            arguments.remove(index);
            arguments.remove(index);
        }

        if(arguments.contains(ARG_MONTHS_SINCE)) {
            int index = arguments.indexOf(ARG_MONTHS_SINCE);
            String monthCountStr;
            try {
                String rawMonthCountStr = arguments.get(index + 1);
                if(rawMonthCountStr == null) {
                    throw new Exception();
                }
                monthCountStr = rawMonthCountStr;
            } catch (Exception e) {
                System.err.println("[ERROR] Requested past months but did no month count was supplied.");
                System.err.println("        Expected: [ <" + ARG_MONTHS_SINCE + "> <N> ]");
                return;
            }
            int monthCount = 0;
            try {
                monthCount = Integer.parseInt(monthCountStr);
                if(monthCount < 1) {
                    throw new Exception();
                }
            } catch (Exception e) {
                System.err.println("[ERROR] Requested past months but (" + monthCountStr + ") could not be parsed to a natural number (1, 2, 3, ...)");
                System.err.println("        Expected: [ <" + ARG_MONTHS_SINCE + "> <N> ]");
                return;
            }
        }

        if(arguments.contains(ARG_INP_FORMAT)) {
            int index = arguments.indexOf(ARG_INP_FORMAT);
            try {
                String formatStr = arguments.get(index + 1);
                if(formatStr == null) {
                    throw new Exception();
                }
                if(formatStr == ARG_CSV) {
                    inFormat = Format.CSV;
                } else if(formatStr == ARG_XML) {
                    inFormat = Format.XML;
                } else {
                    System.err.println("[ERROR] Input format was requested but (" + formatStr + ") could not be parsed as a valid format");
                    System.err.println("        Expected: [ <" + ARG_INP_FORMAT + "> <" + ARG_CSV + "|" + ARG_XML +"> ]");
                    return;
                }
            } catch(Exception e) {
                System.err.println("[ERROR] Input format was requested but not specified");
                System.err.println("        Expected: [ <" + ARG_INP_FORMAT + "> <" + ARG_CSV + "|" + ARG_XML +"> ]");
                return;
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
                if(formatStr == ARG_CSV) {
                    outFormat = Format.CSV;
                } else if(formatStr == ARG_XML) {
                    outFormat = Format.XML;
                } else {
                    System.err.println("[ERROR] Output format was requested but (" + formatStr + ") could not be parsed as a valid format");
                    System.err.println("        Expected: [ <" + ARG_OUT_FORMAT + "> <" + ARG_CSV + "|" + ARG_XML +"> ]");
                    return;
                }
            } catch(Exception e) {
                System.err.println("[ERROR] Output format was requested but not specified");
                System.err.println("        Expected: [ <" + ARG_OUT_FORMAT + "> <" + ARG_CSV + "|" + ARG_XML +"> ]");
                return;
            }
            arguments.remove(index);
            arguments.remove(index);
        }

        if(flags.contains(Flag.DEBUG)) {
            if(arguments.size() > 2) {
                System.err.println("Too many arguments, some will be unused.");
            }
        }

        if(arguments.size() > 0) {
            in = arguments.get(0);
            arguments.remove(0);
        }

        if(arguments.size() > 0) {
            out = arguments.get(0);
            arguments.remove(0);
        }

        //Verify/Load input file
        try {
            in = (in == null) ? DEFAULT_IN_FILE : in;
            stations = loadInputList(inFormat, in, flags);
        } catch (Exception e) {
            System.err.println("[ERROR] Could not load file \"" + in +"\"");
            return;
        }

        //Verify output path
        //If the definition of DEFAULT_OUT_PATH is changed, that directory will need to be verified as well
        out = (out == null) ? DEFAULT_OUT_PATH : out;
        if(!DEFAULT_OUT_PATH.equals(out)) {
            File file = new File(out);
            if(file.exists()) {
                if(file.isFile() || !file.isDirectory()) {
                    System.err.println("[ERROR] Requested output path is an existing file (not a directory).");
                    return;
                }
            } else {
                file.mkdirs();
                if(!file.isDirectory()) {
                    System.err.println("[ERROR] Error while attempting to create requested output directories.");
                    return;
                }
            }
        }

        //Run networking
        (new Client(out, outFormat, datum, startDate, endDate, flags)).run(stations);
    }

    public static List<String> loadInputList(Format format, String fileStr, EnumSet flags) throws Exception {
        ArrayList<String> ids = new ArrayList<String>();
        File inFile = new File(fileStr);
        if(!inFile.exists()) {
            throw new Exception();
        }
        if(!inFile.isFile()) {
            throw new Exception();
        }

        //default format is comma separated values
        if(format == null || format == Format.CSV) {
            FileReader inFileReader = new FileReader(fileStr);
            BufferedReader bufferedReader = new BufferedReader(inFileReader);

            String inLine = "";
            
            while((inLine = bufferedReader.readLine()) != null) {
                inLine.replaceAll("\\s+", "");  //remove all whitespace
                String[] subStrings = inLine.split(","); //split on comma
                if(subStrings[0] != null) {
                    try {
                        Integer.parseInt(subStrings[0]);//ensure that the number is some valid integer
                        ids.add(subStrings[0]);         //first value on each row is NOAA database station id
                    } catch (Exception e) {
                        if(flags != null) {
                            if(flags.contains(Flag.DEBUG)) {
                                System.err.println("[err] input file contained a malformed line");
                            }
                        }
                    }   
                }    
            }
        } else if (format == Format.CSV) {
            //TODO XML input file implementation
        }

        if(flags != null) {
            if(flags.contains(Flag.DEBUG)) {
                System.out.print("\nList of station ids:\n");
                for(String s : ids) {
                    System.out.println(s + ",");
                }
            }
        }

        return ids;
    }

    public static String currentTimeToString(String timezone) {
        //Be wary of Calendar, much is deprecated and many limitations exist
        //In this simple use case it is quite acceptable, but if this code is repurposed a newer Date library might need to be implemented
        DateFormat dateFormat = new SimpleDateFormat(NOAAXML.DATE_FORMAT);
        dateFormat.setTimeZone(TimeZone.getTimeZone(timezone));
        Calendar calendar = Calendar.getInstance();
        return dateFormat.format(calendar.getTime());
    }

    public Client(String out, Format outFormat, String datum, String startDate, String endDate, EnumSet <Flag> flags) {
        mRequestDatum = (datum == null) ? DEFAULT_DATUM : datum;
        mRequestStartDate = (startDate == null) ? DEFAULT_DATE_BEGIN : startDate;
        mRequestEndDate = (endDate == null) ? currentTimeToString(NOAAXML.DATABASE_TIMEZONE) : endDate;
        mOutputPath = out;
        mOutputFormat = (outFormat == null) ? Format.CSV : outFormat;
        mFlags = (flags == null) ? EnumSet.of(Flag.NIL) : flags;

        if(mFlags.contains(Flag.DEBUG)) {
            System.out.println("\nClient information:");
            System.out.println("Datum      : " + mRequestDatum);
            System.out.println("Start      : " + mRequestStartDate);
            System.out.println("End        : " + mRequestEndDate);
            System.out.println("Output Dir : " + mOutputPath);
            if(mOutputFormat == Format.CSV) {
                System.out.println("Output format : CSV");
            } else if (mOutputFormat == Format.XML) {
                System.out.println("Output format : XML");
            }
            System.out.print("Flags: DEBUG");
            if(mFlags.contains(Flag.VERBOSE)) {
                System.out.print("|VERBOSE");
            }
            if(mFlags.contains(Flag.SUPPRESS)) {
                System.out.print("|SUPPRESS");
            }
            if(mFlags.contains(Flag.RAW)) {
                System.out.print("|RAW");
            }
        }
    }

    public void run(List<String> stations) {
        for(String id : stations) {
            try {
                SOAPConnectionFactory soapConnectionFactory = SOAPConnectionFactory.newInstance();
                SOAPConnection connection = soapConnectionFactory.createConnection();
                SOAPMessage message = prepareMessage(id);

                System.out.print("\nPrinting the message that is being sent: \n\n");
                message.writeTo(System.out);
                System.out.println("\n\n");

                URL endpoint = new URL("http://opendap.co-ops.nos.noaa.gov/axis/services/WaterLevelVerifiedMonthly");
                SOAPMessage response = connection.call(message, endpoint);
                connection.close();

                /*
                System.out.println("\nPrinting the respone that was recieved: \n\n" );
                response.writeTo(System.out);
                 */

                //Uncoment this part if you want the response to be saved locally in an XML file
                /*
                FileOutputStream fout = new FileOutputStream ("SoapResponse.xml");
                response.writeTo(fout);
                fout.close();
                 */

                //You can also stores the response as a String
                /*
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                response.writeTo( out );
                String str = out.toString();  
                 */

                System.out.println("\n\nIterating through the response object to get the values:\n\n");
                SOAPBody responseBody = response.getSOAPBody();

                //Checking for errors
                if (responseBody.hasFault()) {
                    SOAPFault fault = responseBody.getFault();
                    String actor = fault.getFaultActor();
                    System.out.println("Fault contains: ");
                    System.out.println("Fault code: " + fault.getFaultCodeAsName().getQualifiedName());
                    System.out.println("Fault string: " + fault.getFaultString());
                    if (actor != null) {
                        System.out.println("Actor: " + actor);
                    }
               } else {
                    Iterator iterator = responseBody.getChildElements();
                    Iterator iterator2 = null;
                    Iterator iterator3 = null;
                   
                    String tagName = null;
                    SOAPElement se = null;

                    if (iterator.hasNext()) {
                        se = (SOAPElement) iterator.next();
                        iterator = se.getChildElements();
                        while (iterator.hasNext()) {
                            se = (SOAPElement) iterator.next();
                            printMetadata(se);
                            tagName = se.getElementName().getLocalName();
                            if ("data".equals(tagName)) {
                                iterator2 = se.getChildElements();
                                while (iterator2.hasNext()) {
                                    se = (SOAPElement) iterator2.next();
                                    iterator3 = se.getChildElements();
                                    while (iterator3.hasNext()) {
                                        se = (SOAPElement) iterator3.next();
                                        printData(se);
                                    }
                                }
                            }
                        }
                    }
                }

            } catch (SOAPException e) {
                System.err.println("ERROR: ******* " + e.toString());
            } catch (IOException io) {
                System.err.println("ERROR: ******* " + io.toString());
            }
        }
    }

    public SOAPMessage prepareMessage(String stationId) {
        SOAPMessage message = null;
        try {
            SOAPFactory soapFactory = SOAPFactory.newInstance();

            MessageFactory factory = MessageFactory.newInstance();
            message = factory.createMessage();

            SOAPBody body = message.getSOAPBody();
            Name bodyName = soapFactory.createName("getWLVerifiedMonthlyAndMetadata", "water", "http://opendap.co-ops.nos.noaa.gov/axis/webservices/waterlevelverifiedmonthly/wsdl");
            SOAPBodyElement bodyElement = body.addBodyElement(bodyName);

            //Constructing the body for the request
            Name name = soapFactory.createName(NOAAXML.ELEM_STATION_ID);
            SOAPElement symbol = bodyElement.addChildElement(name);
            symbol.addTextNode(stationId);
            name = soapFactory.createName(NOAAXML.ELEM_DATE_BEGIN);
            symbol = bodyElement.addChildElement(name);
            symbol.addTextNode("20160301 00:00");
            name = soapFactory.createName(NOAAXML.ELEM_DATE_END);
            symbol = bodyElement.addChildElement(name);
            symbol.addTextNode(mRequestEndDate);
            name = soapFactory.createName(NOAAXML.ELEM_DATUM);
            symbol = bodyElement.addChildElement(name);
            symbol.addTextNode("MLLW");
            name = soapFactory.createName(NOAAXML.ELEM_UNIT);
            symbol = bodyElement.addChildElement(name);
            symbol.addTextNode("0");
            name = soapFactory.createName(NOAAXML.ELEM_TIME_ZONE);
            symbol = bodyElement.addChildElement(name);
            symbol.addTextNode("0");
        } catch (SOAPException e) {
            System.err.println("ERROR: ******* " + e.toString());
        }
        return message;
    }

    public static void printMetadata(SOAPElement se) {
        String tagName = se.getElementName().getLocalName();
        if (tagName != null) {
            if ("stationId".equals(tagName)) {
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


}


