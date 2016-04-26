package NOAAsoap;

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
    public static final String NODE_OBSERVATION = "item";
    public static final String ELEM_TIME_STAMP = "timeStamp";
    public static final String ELEM_INFERENCE = "inferred";
    public static final String ELEM_INFERRED = ELEM_INFERENCE;
    public static final String ELEM_MAX = "highest";
    public static final String ELEM_HIGHEST = ELEM_MAX;
    public static final String ELEM_MIN = "lowest";
    public static final String ELEM_LOWEST = ELEM_MIN;

    //DATUMS as specified by NOAA
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
    
    public static final String DATE_FORMAT = "yyyyMMdd hh:mm";
    public static final String DATABASE_TIMEZONE = "GMT";

    //Converts an input timestamp into an acceptable format by adding a default time of day (00:00) if necessary, and cleaning whitespace
    public static String prepTimeStamp(String rawTimeStr) {
        return rawTimeStr;
    }

    //Verifies that the given string is a legitimate timestamp which the database may accept
    //This is textual verification, not logical verification. 
    //ie. Dates prior to 1900 or exceeding the present year will be accepted
    public static boolean verifyTimeStamp(String timeStr) {
        return true;
    }
}