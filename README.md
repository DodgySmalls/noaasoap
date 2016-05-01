# NOAAQuery

A simple tool for querying the NOAA sea level database. This tool could easily be adapted to fit other datasets, but only monthly values can be polled. NOAAQuery can be run as follows (where [elem] is an option (0 or 1), <elem> is exactly once, and {is 0 or more}):

	NOAAQuery [INPUT_FILENAME [OUTPUT_DIRECTORY]] {ARGS} {MARGS <VALUE>}

ARGS may be any of the following:

	-debug              :  enables the debug console
	-verbose            :  enables additional console output
	-suppress-console   :  overrides other console commands, and silences normal console use, errors will still print
	-suppress-errors    :  stops errors from printing, useful if you are piping console output, but not recommended

	-raw                :  outputs raw responses from the NOAA server to files with the name STATIONID_response.xml
	-raw-only           :  disables normal file output, but still outputs the raw responses as above
	-suppres-files      :  no files will be written

MARGS may be any of the following, but must be immediately followed by the value they use:

		-datum <NOAA_DATUM> : 
		specify the datum to retrieve (https://tidesandcurrents.noaa.gov/datum_options.html) (default: MSL)
		
		-from <DATE>        : 
		specify the start point of the data to retrieve (default: 1960/01/01)														
		-to   <DATE>        :
		specify the end point of the data to retrieve (default: CURRENT_DATE)
		
		-months <N>         : 
		specify the retrieval of datapoints of N months before the present

		-inpf <XML|CSV>     : 
		specify the format of the file to be read (default: CSV)
		
		-outf <XML|CSV>     : 
		specify the format of the file to be written (default: CSV)


The default operation of NOAAQuery reads a comma separated list, retrieves MSL data from 1960/01/01, and writes the values to a comma separated list

Notes on in/out files:

The use of "CSV" is loose. 
NOAAQuery will read a comma separated list with one item per line, where the NOAA stationId is the first item of that list.

		NOAA_ID, anything, anything, ...
		NOAA_ID, anything, anything, ...
		...

The output "CSV" format is:
	
		YYYY MM DD, DATUM_RETRIEVED
		YYYY MM DD, DATUM_RETRIEVED
		...

The input XML format traverses XML looking for any nodes labelled "NOAAid", and reads their values

		<data>
			<etc>
				...
				<NOAAid>NOAA_ID</NOAAid>
				<NOAAid>NOAA_ID</NOAAid>
				...
			</etc>
		</data>

The output XML format is:

		<data>
			<date>YYYYMMDD</date>
			<value>N</value>
		</data>
		<data>
			<date>YYYYMMDD</date>
			<value>N</value>
		</data>
