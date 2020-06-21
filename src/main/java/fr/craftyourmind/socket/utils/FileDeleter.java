//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package fr.craftyourmind.socket.utils;

import java.io.File;

class FileDeleter {
    private int count = 0;
    private File f = null;

    public FileDeleter(File f) {
        this.f = f;
    }

    public int delete() {
        this.deleteFile(this.f);
        return this.getDeletedCount();
    }

    private void deleteFile(File fi) {
        if (fi.isDirectory()) {
            File[] var2 = fi.listFiles();
            int var3 = var2.length;

            for(int var4 = 0; var4 < var3; ++var4) {
                File ff = var2[var4];
                this.deleteFile(ff);
            }
        }

        fi.delete();
        ++this.count;
    }

    public int getDeletedCount() {
        return this.count;
    }
}
