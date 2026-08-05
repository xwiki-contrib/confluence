/*
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.xwiki.contrib.confluence.filter.internal;

import org.apache.commons.configuration2.ex.ConfigurationException;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.io.input.BoundedInputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xwiki.contrib.confluence.filter.input.ConfluenceProperties;
import org.xwiki.contrib.confluence.filter.internal.input.ConfluenceCanceledException;
import org.xwiki.filter.FilterException;
import org.xwiki.job.event.status.JobProgressManager;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipException;

import static org.xwiki.contrib.confluence.filter.input.ConfluenceXMLPackage.KEY_ID;

/**
 * A Confluence CSV backup is a ZIP archive containing CSV files with the .gz extension but that are plain text,
 * not gzipped CSV files. This class represents such files.
 * Some CSV files are also found in the legacy XML backups, but without the .gz extension.
 * In some backups, the files are in the data folder of the archive (site backup?), in some they are directly at the
 * root (space backups?)
 * This class abstracts these differences fact by first checking the file at the given path, and then at the given path
 * with an additional .gz suffix, and then same thing under the data folder.
 *
 * @since 9.96.0
 * @version $Id$
 */
public class ConfluenceCSVFile implements ConfluenceRecordReader
{
    private static final String DOT_GZ = ".gz";
    private static final String DOT_CSV = ".csv";
    private static final Logger LOGGER = LoggerFactory.getLogger(ConfluenceCSVFile.class);

    private final File backupFolder;
    private final String tableName;
    private final ConfluenceCSVFieldMapping fieldMapping;

    private CSVRecord currentRecord;

    /**
     * @param backupFolder the root of the backup directory in which to find the file
     * @param basename the basename of the file (with or without its extension(s))
     */
    public ConfluenceCSVFile(File backupFolder, String basename)
    {
        this.backupFolder = backupFolder;
        String tname = basename;
        if (tname.endsWith(DOT_GZ)) {
            tname = tname.substring(0, tname.length() - 3);
        }

        if (tname.endsWith(DOT_CSV)) {
            tname = tname.substring(0, tname.length() - 4);
        }

        this.tableName = tname;

        ConfluenceCSVFieldMapping m;
        try {
            m = Enum.valueOf(ConfluenceCSVTable.class, this.tableName).fieldMapping;
        } catch (IllegalArgumentException e) {
            // We didn't find a mapping for this table. We will map field names to themselves, and we will consider
            // the first column as the id.
            // This is for example the case for tasks, which are also exported as CSV in XML exports
            m = null;
        }
        this.fieldMapping = m;
    }

    /**
     * @return a buffered reader to read the content of the file, or null if the file doesn't exist.
     * @throws IOException if something wrong happen.
     */
    public BufferedReaderWithSizeAndPosition getBufferedReader() throws IOException
    {
        File file = getExistingFile();
        if (file == null) {
            return null;
        }

        BoundedInputStream bis = new BoundedInputStream(new FileInputStream(file), file.length());
        try {
            if (file.getName().endsWith(DOT_GZ)) {
                try {
                    GZIPInputStream gis = new GZIPInputStream(bis);
                    InputStreamReader reader = new InputStreamReader(gis);
                    return new BufferedReaderWithSizeAndPosition(reader, bis);
                } catch (ZipException e) {
                    // fallback to reading plain text
                    bis.close();
                    bis = new BoundedInputStream(new FileInputStream(file), file.length());
                }
            }
        } catch (IOException e) {
            bis.close();
            throw e;
        }

        Reader reader = new InputStreamReader(bis, StandardCharsets.UTF_8);
        return new BufferedReaderWithSizeAndPosition(reader, bis);
    }

    /**
     * @return a File instance if the file is found, or null if the file doesn't exist at the suspected locations
     */
    public File getExistingFile()
    {
        return getExistingFile(backupFolder, tableName);
    }

    private static File getExistingFile(File backupFolder, String filename)
    {
        String filenameCSV = filename;
        if (!filename.endsWith(DOT_CSV)) {
            filenameCSV += DOT_CSV;
        }

        File file = new File(backupFolder, filenameCSV);
        if (file.exists()) {
            return file;
        }

        String filenameWithGZIP = filenameCSV + DOT_GZ;
        file = new File(backupFolder, filenameWithGZIP);
        if (file.exists()) {
            return file;
        }

        File dataFolder = new File(backupFolder, "data");
        file = new File(dataFolder, filenameCSV);
        if (file.exists()) {
            return file;
        }

        file = new File(dataFolder, filenameWithGZIP);
        if (file.exists()) {
            return file;
        }

        return null;
    }

    /**
     * @return whether the file exists
     */
    public boolean exists()
    {
        return getExistingFile() != null;
    }

    /**
     * Call the given callback on each record of this file.
     * @param progress the job progress manager, to update progress as the records are being read
     * @param recordConsumer a method that will be called with each record in this CSV file
     * @throws IOException if something wrong happens
     * @throws ConfluenceCanceledException if a request to interrupt the execution has fired
     */
    public void readRecords(JobProgressManager progress, ConfluenceConsumer recordConsumer)
            throws ConfigurationException, FilterException, IOException, ConfluenceCanceledException
    {
        try (BufferedReaderWithSizeAndPosition reader = this.getBufferedReader()) {
            if (reader == null) {
                LOGGER.warn("Could not read [{}]", tableName);
                return;
            }
            long length = (int) reader.length();
            progress.pushLevelProgress(100, this);
            CSVParser p = CSVFormat.DEFAULT.builder().setHeader().setSkipHeaderRecord(true).build().parse(reader);
            progress.startStep(this);
            int percent = 0;
            for (CSVRecord r : p) {
                int newPercent = (int) (100 * (reader.position() / length));
                while (percent < newPercent) {
                    percent++;
                    progress.startStep(this);
                }
                this.currentRecord = r;
                recordConsumer.accept(this);
            }
            progress.endStep(this);
            progress.popLevelProgress(this);
        }
    }

    /**
     * Reads the records in the CSV file corresponding to the given basename. The provided record consumer is called for
     * each record. Table bodycontent is special-cased to abstract away the potentially complicated encoding of its
     * body field.
     * @param progress the job progress manager, to update progress as the records are being read
     * @param backupFolder the folder containing the Confluence export unzipped
     * @param basename the name of the table
     * @param recordConsumer the record consumer
     * @throws ConfigurationException if something wrong happens
     * @throws FilterException if something wrong happens
     * @throws IOException if something wrong happens
     * @throws ConfluenceCanceledException is the execution is to be interrupted
     */
    public static void readRecords(JobProgressManager progress, File backupFolder, String basename,
        ConfluenceConsumer recordConsumer)
            throws ConfigurationException, FilterException, IOException, ConfluenceCanceledException
    {
        (ConfluenceCSVTable.bodycontent.name().equals(basename)
               ? new BodyContentCSVFile(backupFolder, basename)
               : new ConfluenceCSVFile(backupFolder, basename)
        ).readRecords(progress, recordConsumer);
    }

    @Override
    public Object readRecord(ConfluenceProperties properties)
            throws FilterException
    {
        try {
            if (fieldMapping == null) {
                for (String fieldName : currentRecord) {
                    properties.addProperty(fieldName, get(fieldName));
                }
                return currentRecord.get(0);
            }

            for (Map.Entry<String, String> csvXmlPair : fieldMapping.entrySet()) {
                String csvField = csvXmlPair.getValue();
                if (currentRecord.isSet(csvField)) {
                    String value = get(csvField);
                    properties.addProperty(csvXmlPair.getKey(), value);
                }
            }

            String id = currentRecord.get(fieldMapping.getCSVFieldId());
            properties.addProperty(KEY_ID, id);
            return id;
        } catch (IOException e) {
            throw new FilterException(e);
        }
    }

    @Override
    public String getType()
    {
        return tableName;
    }

    /**
     * @return the value of the field
     * @param field the field to read
     * @throws IOException if something wrong happens while reading the field
     */
    public String get(String field) throws IOException
    {
        return currentRecord.get(field);
    }

    /**
     * @return whether the given table exists in the given directory
     * @param directory the unzipped CSV Confluence export folder in which to find the table
     * @param filename the filename of the table (with its .csv and .gz extensions or not)
     */
    public static boolean exists(File directory, String filename)
    {
        return getExistingFile(directory, filename) != null;
    }
}
