package com.appstronautstudios.filemanager;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Environment;
import android.util.Log;
import android.webkit.MimeTypeMap;

import com.opencsv.CSVReader;
import com.opencsv.CSVWriter;

import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Locale;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public class FileUtils {

    public static String getInternalFilePath(Context context, String envRoot, String filename) {
        // https://stackoverflow.com/questions/10123812/difference-between-getexternalfilesdir-and-getexternalstoragedirectory
        // https://developer.android.com/training/data-storage/app-specific#java
        // this will return a path to a file in your package folder. Files here are encrypted as of
        // api 29 and won't be visible to the default folder browser app (afaik). You can still share
        // them using the FileProvider.
        File folderRoot = context.getExternalFilesDir(envRoot);
        folderRoot.mkdirs(); // need this for storage delete or (untested) fresh install
        if (filename != null) {
            return folderRoot + "/" + filename;
        } else {
            return folderRoot.getPath();
        }
    }

    public static void writeJsonToFile(JSONObject json, File file) {
        try {
            Writer output;
            output = new BufferedWriter(new FileWriter(file));
            output.write(json.toString());
            output.close();
        } catch (Exception e) {
            Log.d("", e.getLocalizedMessage());
        }
    }

    public static byte[] getBytes(File file) throws IOException {
        byte[] bytes = new byte[(int) file.length()];
        BufferedInputStream bis = new BufferedInputStream(new FileInputStream(file));
        DataInputStream dis = new DataInputStream(bis);
        dis.readFully(bytes);
        return bytes;
    }

    public static String sanitizeStringForCSV(String input) {
        return input.replace("\"", "").replace("\'", "").replace("\\", "");
    }

    public static File exportDb(Activity activity, SQLiteDatabase db, String table, String sortKey, String[] dateHeaders, String filename) {
        OutputStream outputStream;
        File tempFile = null;

        try {
            // https://stackoverflow.com/a/62879112/740474
            // Creating Temp file
            tempFile = new File(FileUtils.getInternalFilePath(activity, Environment.DIRECTORY_DOCUMENTS, filename));
            tempFile.createNewFile();
            outputStream = new FileOutputStream(tempFile);

            // write csv to stream
            CSVWriter csvWrite = new CSVWriter(new OutputStreamWriter(outputStream));
            Cursor cursor;
            if (sortKey == null) {
                cursor = db.rawQuery("SELECT * FROM " + table, null);
            } else {
                cursor = db.rawQuery("SELECT * FROM " + table + " ORDER BY " + sortKey + " DESC", null);
            }
            String[] columnNames = cursor.getColumnNames();
            csvWrite.writeNext(columnNames);
            while (cursor.moveToNext()) {
                // get array of columns you want to export
                ArrayList<String> columns = new ArrayList<>();
                for (int i = 0; i < cursor.getColumnCount(); i++) {
                    if (dateHeaders != null && Arrays.asList(dateHeaders).contains(columnNames[i])) {
                        // date column. Convert to readable format
                        columns.add(timestampToCsvDate(Long.parseLong(cursor.getString(i))));
                    } else {
                        // sanitize column string and add to csv writer
                        String column = cursor.getString(i);
                        if (column == null) column = "";
                        column = sanitizeStringForCSV(column);
                        columns.add(column);
                    }
                }
                csvWrite.writeNext(columns.toArray(new String[0]));
            }
            csvWrite.close();
            cursor.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return tempFile;
    }

    public static void shareFile(Activity activity, Uri uri, String mimeType) {
        // file to receiving app
        Intent shareIntent = new Intent();
        shareIntent.setAction(Intent.ACTION_SEND);
        shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION); // temp permission for receiving app to read this file
        if (mimeType != null) shareIntent.setDataAndType(uri, mimeType);
        shareIntent.putExtra(Intent.EXTRA_STREAM, uri);
        activity.startActivity(Intent.createChooser(shareIntent, "Choose an app"));
    }

    public static void zip(ArrayList<File> files, String zipPath) {
        // https://stackoverflow.com/a/25562488/14101362
        // WARNING - when trying to open this on desktop this seems to try to open in UTF-16.
        // unsure if this is problem related to converting a file to a byte array, the way csvs are
        // saved or both
        try {
            FileOutputStream dest = new FileOutputStream(zipPath);
            ZipOutputStream zos = new ZipOutputStream(new BufferedOutputStream(dest));
            for (File file : files) {
                Log.v("Compress", "Adding: " + file.getPath());
                if (file.exists()) {
                    ZipEntry entry = new ZipEntry(file.getName());
                    zos.putNextEntry(entry);
                    zos.write(getBytes(file));
                    zos.closeEntry();
                }
            }
            zos.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static boolean unzip(Context context, Uri inputZipPath, String defaultLocation, String photoLocation) {
        InputStream is;
        ZipInputStream zis;
        try {
            String filename;
            is = context.getContentResolver().openInputStream(inputZipPath);
            zis = new ZipInputStream(new BufferedInputStream(is));
            ZipEntry ze;
            byte[] buffer = new byte[1024];
            int count;

            while ((ze = zis.getNextEntry()) != null) {
                filename = ze.getName();

                // Need to create directories if not exists, or
                // it will generate an Exception...
                if (ze.isDirectory()) {
                    File fmd = new File(defaultLocation + "/" + filename);
                    fmd.mkdirs();
                    continue;
                }

                // determine where this file should be saved
                FileOutputStream fOut;
                String mimeType = getMimeType(filename);
                if (photoLocation != null && mimeType.contains("image/")) {
                    fOut = new FileOutputStream(photoLocation + "/" + filename);
                } else {
                    fOut = new FileOutputStream(defaultLocation + "/" + filename);
                }

                // write to this location
                while ((count = zis.read(buffer)) != -1) {
                    fOut.write(buffer, 0, count);
                }

                // closer write and entry
                fOut.close();
                zis.closeEntry();
            }
            // close input stream
            zis.close();
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }

        return true;
    }

    public static ArrayList<String[]> parseRowsFromCsv(Context context, Uri uri) {
        ArrayList<String[]> allLines = new ArrayList<>();
        try {
            CSVReader csvRead;
            if ("content".equals(uri.getScheme())) {
                // content stream for downloads documents etc.
                InputStreamReader inputStreamReader = new InputStreamReader(context.getContentResolver().openInputStream(uri));
                csvRead = new CSVReader(inputStreamReader);
            } else {
                File file = new File(uri.getPath());
                FileReader fileReader = new FileReader(file);
                csvRead = new CSVReader(fileReader);
            }
            // collect all lines in an arraylist
            allLines = new ArrayList<>();
            String[] csvLine;
            while ((csvLine = csvRead.readNext()) != null) {
                allLines.add(csvLine);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return allLines;
    }

    public static String getMimeType(String url) {
        // https://stackoverflow.com/a/8591230/740474
        String type = null;
        String extension = MimeTypeMap.getFileExtensionFromUrl(url);
        if (extension != null) {
            type = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
        }
        return type;
    }

    public static String timestampToCsvDate(long timestamp) {
        Date date = new Date(timestamp);
        String outDate = null;
        try {
            SimpleDateFormat fmtOut = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US);
            outDate = fmtOut.format(date);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return outDate;
    }

    public static Date csvDateToDateObject(String dateString) {
        Date outDate = null;
        try {
            SimpleDateFormat fmtOut = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US);
            outDate = fmtOut.parse(dateString);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return outDate;
    }
}
