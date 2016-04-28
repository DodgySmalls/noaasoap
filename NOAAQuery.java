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

public class NOAAQuery {
	public static final String DEFAULT_DATUM = NOAAXML.DATUM_MSL;
	public static final String DEFAULT_DATE_BEGIN = "20160101 00:00"; //TODO UPDATE DEFAULT TO 1960
    public static final String DEFAULT_IN_FILE = "stationlist.dat";
    public static final String DEFAULT_OUT_PATH = "./";

    public static final String ARG_DEBUG = "-debug";
    public static final String ARG_VERBOSE = "-verbose";
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
		if(parseArguments(parseFlags(new CaseInsensitiveList(Arrays.asList(args)))).size() > 0){
			printDebug("Excess arguments");
		}

		defaults(); //values which weren't specified are loaded

		printDebug(inFileDesc.toString());
		printDebug(outFileDesc.toString());
		printDebug(request.toString());

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
        	globalFlags.remove(Flag.ENABLE_ERROR_CONSOLE);
        	globalFlags.remove(Flag.ENABLE_VERBOSE_CONSOLE);
        	globalFlags.remove(Flag.ENABLE_DEBUG_CONSOLE);
        	arguments.remove(ARG_SUPPRESS_CONSOLE);
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
                printError("[ERROR] Starting date was requested but (" + rawTimeStr + ") could not be parsed as a valid timestamp");
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
                printError("[ERROR] End date was requested but (" + rawTimeStr + ") could not be parsed as a valid timestamp");
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
                    printError("[ERROR] Input format was requested but (" + formatStr + ") could not be parsed as a valid format");
                    printError("        Expected: [ <" + ARG_INP_FORMAT + "> <" + ARG_CSV + "|" + ARG_XML +"> ]");
                    System.exit(0);
                }
            } catch(Exception e) {
                printError("[ERROR] Input format was requested but not specified");
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
                    inFileDesc.setFormat(FileDescription.Format.CSV);
                } else if(ARG_XML.equals(formatStr.toUpperCase())) {
                    inFileDesc.setFormat(FileDescription.Format.XML);
                } else {
                    printError("[ERROR] Output format was requested but (" + formatStr + ") could not be parsed as a valid format");
                    printError("        Expected: [ <" + ARG_OUT_FORMAT + "> <" + ARG_CSV + "|" + ARG_XML +"> ]");
                    System.exit(0);
                }
            } catch(Exception e) {
                printError("[ERROR] Output format was requested but not specified");
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