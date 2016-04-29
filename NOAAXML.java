package NOAAsoap;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.TimeZone;

public class NOAAXML {
    public static final String ELEM_STATION_ID = "stationId";
    public static final String ELEM_STATION_NAME = "stationName";
    public static final String ELEM_LATITUDE = "latitude";
    public static final String ELEM_LONGITUDE = "longitude";
    public static final String ELEM_STATE = "state";

    public static final String ELEM_DATA_SOURCE = "dataSource";
    public static final String ELEM_DATE_BEGIN = "beginDate";
    public static final String ELEM_DATE_END = "endDate";
    public static final String ELEM_DATUM = "datum";
    public static final String ELEM_UNIT = "unit";
    public static final String ELEM_TIME_ZONE = "timeZone";

    public static final String NODE_DATA = "data";
    public static final String NODE_ITEM = "item";
    public static final String ELEM_TIME_STAMP = "timeStamp";
    public static final String ELEM_INFERENCE = "inferred";
    public static final String ELEM_INFERRED = ELEM_INFERENCE;
    public static final String ELEM_MAX = "highest";
    public static final String ELEM_HIGHEST = ELEM_MAX;
    public static final String ELEM_MIN = "lowest";
    public static final String ELEM_LOWEST = ELEM_MIN;

    //Datums as specified by NOAA
    //            https://tidesandcurrents.noaa.gov/datum_options.html
    public static final String DATUM_HAT = "HAT";
    public static final String DATUM_HIGHEST_ASTRONOMICAL_TIDE = DATUM_HAT;
    public static final String DATUM_MHHW = "MHHW";
    public static final String DATUM_MEAN_HIGHER_HIGH_WATER = DATUM_MHHW;
    public static final String DATUM_MHW = "MHW";
    public static final String DATUM_MEAN_HIGH_WATER = DATUM_MHW;
    public static final String DATUM_DTL = "DTL";
    public static final String DATUM_DIURNAL_TIDE_LEVEL = DATUM_DTL;
    public static final String DATUM_MTL = "MTL";
    public static final String DATUM_MEAN_TIDE_LEVEL = DATUM_MTL;
    public static final String DATUM_MSL = "MSL";
    public static final String DATUM_MEAN_SEA_LEVEL = DATUM_MSL;
    public static final String DATUM_MLW = "MLW";
    public static final String DATUM_MEAN_LOW_WATER = DATUM_MLW;
    public static final String DATUM_MLLW = "MLLW";
    public static final String DATUM_MEAN_LOWER_LOW_WATER = DATUM_MLLW;
    public static final String DATUM_LAT = "LAT";
    public static final String DATUM_LOWEST_ASTRONOMICAL_TIDE = DATUM_LAT;
    public static final String DATUM_GT = "GT";
    public static final String DATUM_GREAT_DIURNAL_RANGE = DATUM_GT;
    public static final String DATUM_MN = "MN";
    public static final String DATUM_MEAN_RANGE_OF_TIDE = DATUM_MN;
    public static final String DATUM_DHQ = "DHQ";
    public static final String DATUM_MEAN_DIURNAL_HIGH_WATER_INEQUALITY = DATUM_DHQ;
    public static final String DATUM_DLQ = "DLQ";
    public static final String DATUM_MEAN_DIURNAL_LOW_WATER_INEQUALITY = DATUM_DLQ;
    public static final String DATUM_HWI = "HWI";
    public static final String DATUM_GREENWICH_HIGH_WATER_INTERVAL = DATUM_HWI;
    public static final String DATUM_LWI = "LWI";
    public static final String DATUM_GREENWICH_LOW_WATER_INTERVAL = DATUM_LWI;
    
    public static final String DATE_FORMAT = "yyyyMMdd hh:mm"; //Date format for Java.text.SimpleDateFormat
    public static final String DATABASE_TIMEZONE = "GMT";

    //Converts an input timestamp into an acceptable format by adding a default time of day (00:00) if necessary, and cleaning whitespace
    public static String cleanTimeStamp(String rawTimeStr) {
        return rawTimeStr;
    }

    //Verifies that the given string is a legitimate timestamp which the database may accept
    //This is textual verification, not logical verification. 
    //ie. Dates prior to 1900 or exceeding the present year will be accepted
    public static boolean verifyTimeStamp(String timeStr) {
        return true;
    }

    //Returns the current time as a string formatted for the NOAA database
    public static String currentTimeToString(String timezone) {
        //Be wary of Calendar, much is deprecated and many limitations exist
        //In this simple use case it is quite acceptable, but if this code is repurposed a newer Date library might need to be implemented
        DateFormat dateFormat = new SimpleDateFormat(NOAAXML.DATE_FORMAT);
        dateFormat.setTimeZone(TimeZone.getTimeZone(timezone));
        Calendar calendar = Calendar.getInstance();
        return dateFormat.format(calendar.getTime());
    }

    public static String nMonthsAgoToString(String timezone, int n) {
        DateFormat dateFormat = new SimpleDateFormat(NOAAXML.DATE_FORMAT);
        dateFormat.setTimeZone(TimeZone.getTimeZone(timezone));
        Calendar calendar = Calendar.getInstance();
        final int currentMonth = calendar.get(Calendar.MONTH); //0-11
        final int currentYear  = calendar.get(Calendar.YEAR);

        int year = currentYear - (n / 12);
        int month = currentMonth - (n % 12);

        if(month < 0) {
            year--;
            month = 12 + month;
        }

        calendar.set(Calendar.YEAR, year);
        calendar.set(Calendar.MONTH, month);

        //negates corner cases if hours might roll over to another month in a different timezone
        //the NOAA database returns any data matched with the month of the request
        calendar.set(Calendar.DAY_OF_MONTH, 25);    

        NOAAQuery.printDebug(dateFormat.format(calendar.getTime()));
        return dateFormat.format(calendar.getTime());
    }
    
    
}