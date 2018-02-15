package de.thecode.android.tazreader.importer;

import android.content.Context;
import android.os.Parcel;
import android.os.Parcelable;

import com.dd.plist.PropertyListFormatException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import de.thecode.android.tazreader.R;
import de.thecode.android.tazreader.data.Paper;
import de.thecode.android.tazreader.utils.ReadableException;

/**
 * Created by mate on 17.04.2015.
 */
public class ImportMetadata implements Parcelable {

    private static final String TPAPER_ENTRY = "content.plist";
    private static final String TAZANDROID_ENTRY = "OEBPS/content.opf";

    private static final String KEY_METADATA = "metadata"; // Parent node
    private static final String KEY_SOURCE = "dc:source";
    private static final String KEY_IDENTIFIER = "dc:identifier";
    private static final String KEY_TITLE = "dc:title";
    private static final String KEY_DATE = "dc:date";
    private static final String ATTRIBUTE_ID = "id";
    private static final String ATTRIBUTE_ID_TAZID = "taz-id";
    private static final String ATTRIBUTE_ID_BOOKID = "BookId";

    public enum TYPE {TAZANDROID, TPAPER}

    private String bookId;
    private String archive;
    private String title;
    private String date;
    private TYPE type;

    public String getArchive() {
        return archive;
    }

    public void setArchive(String archive) {
        this.archive = archive;
    }

    public String getBookId() {
        return bookId;
    }

    public void setBookId(String bookId) {
        this.bookId = bookId;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getTitle() {
        return title;
    }

    public void setType(TYPE type) {
        this.type = type;
    }

    public TYPE getType() {
        return type;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getFormatedDate() {
        return date != null ? date.substring(8) + "." + date.substring(5, 7) + "." + date.substring(0, 4) : null;
    }

    public String getTitelWithDate(String seperator) {
        if (getFormatedDate() != null) return getTitle() + " " + seperator + " " + getFormatedDate();
        else return getTitle();

    }

    public String getTitelWithDate(Context context) {
        return getTitelWithDate(context.getString(R.string.string_titel_seperator));
    }


    public static ImportMetadata parse(File file) throws IOException, NotReadableException {

        ZipFile zipFile = new ZipFile(file);

        Enumeration<? extends ZipEntry> zipEntries = zipFile.entries();

        ImportMetadata result = null;
        Throwable lastCause = null;

        while (zipEntries.hasMoreElements() && result == null) {
            ZipEntry zipEntry = zipEntries.nextElement();
            if (zipEntry.getName()
                        .equalsIgnoreCase(TPAPER_ENTRY)) {
                try {
                    result = parseTpaper(zipFile.getInputStream(zipEntry));
                } catch (ParserConfigurationException | ParseException | SAXException | PropertyListFormatException e) {
                    lastCause = e.getCause();
                }
            } else if (zipEntry.getName()
                               .equalsIgnoreCase(TAZANDROID_ENTRY)) {
                try {
                    result = parseTazandroid(zipFile.getInputStream(zipEntry));
                } catch (ParserConfigurationException | SAXException e) {
                    //Log.e();
                    lastCause = e.getCause();
                }
            }
        }
        if (result == null) throw new NotReadableException("No readable taz.app data found", lastCause);

        return result;
    }

    private static ImportMetadata parseTazandroid(InputStream is) throws ParserConfigurationException, IOException, SAXException {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        DocumentBuilder db = dbf.newDocumentBuilder();

        //DocumentBuilder.parse automatically closes InputStream
        Document doc = db.parse(is);

        NodeList metadataNodes = doc.getElementsByTagName(KEY_METADATA);
        if (metadataNodes.getLength() > 0) {
            ImportMetadata result = new ImportMetadata();
            result.setType(TYPE.TAZANDROID);

            Element metadata = (Element) metadataNodes.item(0);

            NodeList sourceNodes = metadata.getElementsByTagName(KEY_SOURCE);
            for (int i = 0; i < sourceNodes.getLength(); i++) {
                Element source = (Element) sourceNodes.item(i);
                if (source.hasAttribute(ATTRIBUTE_ID)) {
                    if (ATTRIBUTE_ID_TAZID.equalsIgnoreCase(source.getAttribute(ATTRIBUTE_ID))) {
                        result.setArchive(source.getFirstChild()
                                                .getTextContent());
                    }
                }
            }

            NodeList identifierNodes = metadata.getElementsByTagName(KEY_IDENTIFIER);
            for (int i = 0; i < identifierNodes.getLength(); i++) {
                Element identifier = (Element) identifierNodes.item(i);
                if (identifier.hasAttribute(ATTRIBUTE_ID)) {
                    if (ATTRIBUTE_ID_BOOKID.equalsIgnoreCase(identifier.getAttribute(ATTRIBUTE_ID))) {
                        result.setBookId(identifier.getFirstChild()
                                                   .getTextContent());
                    }
                }
            }

            parseTitleAndDateFromBookId(result,result.getBookId());

//            NodeList titleNodes = metadata.getElementsByTagName(KEY_TITLE);
//            for (int i = 0; i < titleNodes.getLength(); i++) {
//                Element title = (Element) titleNodes.item(i);
//                result.setTitle(title.getFirstChild()
//                                     .getTextContent());
//            }
//
//            NodeList dateNodes = metadata.getElementsByTagName(KEY_DATE);
//            for (int i = 0; i < dateNodes.getLength(); i++) {
//                Element date = (Element) dateNodes.item(i);
//                result.setDate(date.getFirstChild()
//                                     .getTextContent());
//            }

            return result;
        }

        return null;
    }

    private static ImportMetadata parseTpaper(InputStream is) throws ParserConfigurationException, ParseException, SAXException, PropertyListFormatException, IOException {
        Paper paper = new Paper();
        paper.parsePlist(is, false);
        if (paper.getPlist() != null) {
            ImportMetadata result = new ImportMetadata();
            result.setType(TYPE.TPAPER);
            result.setBookId(paper.getPlist()
                                  .getBookId());
            result.setArchive(paper.getPlist().getArchiveUrl());
            parseTitleAndDateFromBookId(result,paper.getPlist()
                                                    .getBookId());
            return result;
        }
        return null;
    }

    private static void parseTitleAndDateFromBookId(ImportMetadata data, String bookId) {
        if (bookId != null) {
            String[] split = bookId.split("_",2);
            data.setTitle(split[0]);
            data.setDate(split[1].replace("_","-"));
        }
    }


    public static class NotReadableException extends ReadableException {
        public NotReadableException() {
        }

        public NotReadableException(String detailMessage) {
            super(detailMessage);
        }

        public NotReadableException(String detailMessage, Throwable throwable) {
            super(detailMessage, throwable);
        }

        public NotReadableException(Throwable throwable) {
            super(throwable);
        }
    }

    public ImportMetadata() {
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.bookId);
        dest.writeString(this.archive);
        dest.writeString(this.title);
        dest.writeString(this.date);
        dest.writeInt(this.type == null ? -1 : this.type.ordinal());
    }

    private ImportMetadata(Parcel in) {
        this.bookId = in.readString();
        this.archive = in.readString();
        this.title = in.readString();
        this.date = in.readString();
        int tmpType = in.readInt();
        this.type = tmpType == -1 ? null : TYPE.values()[tmpType];
    }

    public static final Creator<ImportMetadata> CREATOR = new Creator<ImportMetadata>() {
        public ImportMetadata createFromParcel(Parcel source) {
            return new ImportMetadata(source);
        }

        public ImportMetadata[] newArray(int size) {
            return new ImportMetadata[size];
        }
    };
}
