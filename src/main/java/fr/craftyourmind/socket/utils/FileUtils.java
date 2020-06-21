//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package fr.craftyourmind.socket.utils;

import java.io.*;

public class FileUtils {
    public FileUtils() {
    }

    public static int deleteFile(File f) {
        FileDeleter fd = new FileDeleter(f);
        fd.delete();
        return fd.getDeletedCount();
    }

    public static void copyFile(File from, File to) throws IOException {
        if (from.isDirectory()) {
            to.mkdir();
            File[] var2 = from.listFiles();
            int var3 = var2.length;

            for(int var4 = 0; var4 < var3; ++var4) {
                File ff = var2[var4];
                copyFile(ff, new File(to.getAbsolutePath() + File.separatorChar + ff.getName()));
            }
        } else {
            to.createNewFile();
            InputStreamReader isr = new InputStreamReader(new FileInputStream(from));
            OutputStreamWriter osw = new OutputStreamWriter(new FileOutputStream(to));

            while(isr.ready()) {
                osw.write(isr.read());
            }

            osw.flush();
            osw.close();
            isr.close();
            System.out.println(from.getAbsolutePath() + " -> " + to.getAbsolutePath());
        }

    }
}
