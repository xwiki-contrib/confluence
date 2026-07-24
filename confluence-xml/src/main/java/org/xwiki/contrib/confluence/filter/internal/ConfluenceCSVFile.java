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

import org.apache.commons.compress.compressors.zstandard.ZstdCompressorInputStream;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xwiki.contrib.confluence.filter.input.ConfluenceProperties;
import org.xwiki.contrib.confluence.filter.internal.input.ConfluenceCanceledException;
import org.xwiki.filter.FilterException;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Base64;
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
public class ConfluenceCSVFile implements ConfluenceObjectReader
{
    private static final String DOT_GZ = ".gz";
    private static final String DOT_CSV = ".csv";
    private static final Logger LOGGER = LoggerFactory.getLogger(ConfluenceCSVFile.class);

    private final File backupFolder;
    private final String filename;
    private final GCJ getCancelledJob;

    private CSVRecord currentRecord;

    @FunctionalInterface
    public interface GCJ
    {
        void apply() throws ConfluenceCanceledException;
    }

    /**
     * @param getCancelledJob the method that checks if the job was canceled
     * @param backupFolder the root of the backup directory in which to find the file
     * @param filename the filename
     */
    public ConfluenceCSVFile(GCJ getCancelledJob, File backupFolder, String filename)
    {
        this.getCancelledJob = getCancelledJob == null ? () -> { } : getCancelledJob;
        this.backupFolder = backupFolder;
        this.filename = filename;
    }

    /**
     * @return a buffered reader to read the content of the file, or null if the file doesn't exist.
     * @throws IOException if something wrong happen.
     */
    public BufferedReader getBufferedReader() throws IOException
    {
        File file = getExistingFile();
        if (file == null) {
            return null;
        }

        if (file.getName().endsWith(DOT_GZ)) {
            try {
                FileInputStream fis = new FileInputStream(file);
                GZIPInputStream gis = new GZIPInputStream(fis);
                return new BufferedReader(new InputStreamReader(gis));
            } catch (ZipException e) {
                // fallback to reading plain text
            }
        }

        return Files.newBufferedReader(file.toPath(), StandardCharsets.UTF_8);
    }

    /**
     * @return a File instance if the file is found, or null if the file doesn't exist at the suspected locations
     */
    public File getExistingFile()
    {
        return getExistingFile(backupFolder, filename);
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
     * @param recordConsumer a method that will be called with each record in this CSV file
     * @throws IOException if something wrong happens
     */
    public void readRecords(ConfluenceConsumer recordConsumer)
            throws ConfigurationException, FilterException, IOException, ConfluenceCanceledException
    {
        try (BufferedReader reader = this.getBufferedReader()) {
            if (reader == null) {
                LOGGER.warn("Could not read [{}]", filename);
                return;
            }
            CSVParser p = CSVFormat.DEFAULT.builder().setHeader().setSkipHeaderRecord(true).build().parse(reader);
            for (CSVRecord r : p) {
                getCancelledJob.apply();
                this.currentRecord = r;
                recordConsumer.accept(this);
            }
        }
    }

    public static void readRecords(GCJ getCancelledJob, File backupFolder, String filename, ConfluenceConsumer recordConsumer)
            throws ConfigurationException, FilterException, IOException, ConfluenceCanceledException
    {
        ("bodycontent".equals(filename)
               ? new BodyContentCSVFile(getCancelledJob, backupFolder, filename)
               : new ConfluenceCSVFile(getCancelledJob, backupFolder, filename)
        ).readRecords(recordConsumer);
    }

    @Override
    public Object readObjectProperties(ConfluenceProperties properties, ConfluenceObjectFields fields)
    {
        for (Map.Entry<String, String> csvXmlPair : fields.entrySet()) {
            String csvField = csvXmlPair.getValue();
            if (currentRecord.isSet(csvField)) {
                String value = currentRecord.get(csvField);
                properties.addProperty(csvXmlPair.getKey(), value);
            }
        }

        String id = currentRecord.get(fields.getCSVFieldId());
        properties.addProperty(KEY_ID, id);
        return id;
    }

    public String get(String field)
    {
        return currentRecord.get(field);
    }

    public static boolean exists(File directory, String filename)
    {
        return getExistingFile(directory, filename) != null;
    }

    static class BodyContentCSVFile extends ConfluenceCSVFile
    {
        /**
         * @param getCancelledJob the method that checks if the job was canceled
         * @param backupFolder    the root of the backup directory in which to find the file
         * @param filename        the filename
         */
        BodyContentCSVFile(GCJ getCancelledJob, File backupFolder, String filename)
        {
            super(getCancelledJob, backupFolder, filename);
        }

        @Override
        public String get(String field)
        {
            String v = super.get(field);
            if ("bodycontent".equals(field) && v != null && v.startsWith("KLUv/")) {
                // that body content is compressed using zstd and then encoded with base64
                // String base64 = StringUtils.rightPad(v, 11, 'A') + '=';
                // byte[] zstdCompressedBodyContent = Base64.getDecoder().decode(base64);
                // FIXME continue
            }
            return v;
        }
    }

}
