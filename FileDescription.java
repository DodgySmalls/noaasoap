package NOAAsoap;

public class FileDescription {
    public enum Format {
        XML, CSV
    }

    private String mDirectoryName, mFileName;
    private Format mFormat = null;
    
    public FileDescription() {
        mDirectoryName = null;
        mFileName = null;
    }

    public FileDescription(String directoryName, String fileName, Format format) {
        this.mDirectoryName = directoryName;
        this.mFileName = fileName;
        this.mFormat = format;
    }

    public String getDirectoryName() {
        return mDirectoryName;
    }
    public void setDirectoryName(String directoryName) {
        mDirectoryName = directoryName;
    }

    public String getFileName() {
        return mFileName;
    }
    public void setFileName(String fileName) {
        mFileName = fileName;
    }

    public Format getFormat() {
        return mFormat;
    }
    public void setFormat(Format format) {
        mFormat = format;
    }

    public void addExtensionToFileName() {
        if(mFileName == null) {
            mFileName = "FILENAME_UNDEFINED";
        }

        if(mFormat == Format.CSV) {
            mFileName += ".dat";
        } else {
            mFileName += ".xml";
        }
    }
    @Override
    public String toString() {
        if(mFormat == Format.XML) {
            return "FileDescription, directory:" + mDirectoryName + ", file:" + mFileName + ", format:XML"; 
        } else {
            return "FileDescription, directory:" + mDirectoryName + ", file:" + mFileName + ", format:CSV"; 
        }
    }
}