package NOAAsoap;

public class Request {
	private String mStation, mDatum, mStart, mEnd;

	public Request() {
		mDatum = null;
		mStation = null;
		mStart = null;
		mEnd = null;
	}

	public String getStation() {
		return mStation;
	}
	public void setStation(String station) {
		mStation = station;
	}

	public String getDatum() {
		return mDatum;
	}
	public void setDatum(String datum) {
		mDatum = datum;
	}

	public String getStart() {
		return mStart;
	}
	public void setStart(String start) {
		mStart = start; 
	}

	public String getEnd() {
		return mEnd;
	}
	public void setEnd(String end) {
		mEnd = end;
	}

	@Override
	public String toString() {
		return "Request, station:" + mStation + ", datum:" + mDatum + ", start:" + mStart + ", end:" + mEnd; 
	}
}