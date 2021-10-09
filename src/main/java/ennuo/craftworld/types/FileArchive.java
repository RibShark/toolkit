package ennuo.craftworld.types;

import ennuo.craftworld.memory.Bytes;
import ennuo.craftworld.memory.Data;
import ennuo.craftworld.memory.Output;
import ennuo.craftworld.resources.io.FileIO;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JProgressBar;

public class FileArchive {

    public static enum ArchiveType {
        FARC,
        FAR4,
        FAR5
    }

    public ArchiveType archiveType = ArchiveType.FARC;

    public File file;

    public boolean isParsed = false;
    public boolean shouldSave = false;

    public byte[] fat;
    public byte[] hashinate = new byte[0x14];

    public byte[] hashTable;
    public long tableOffset;

    public ArrayList<FileEntry> entries = new ArrayList<FileEntry>();
    public ArrayList<FileEntry> queue = new ArrayList<FileEntry>();
    public int queueSize = 0;

    public FileArchive(File file) {
        this.file = file;
        process();
    }

    public void process() {
        System.out.println("Started processing FileArchive located at: " + this.file.getAbsolutePath());
        long begin = System.currentTimeMillis();
        this.hashTable = null;
        this.entries = new ArrayList < FileEntry > ();
        this.queue = new ArrayList < FileEntry > ();
        queueSize = 0;
        isParsed = false;
        int entryCount = 0;
        try {
            RandomAccessFile fishArchive = new RandomAccessFile(this.file.getAbsolutePath(), "rw");
            if (fishArchive.length() < 8) {
                System.out.println("This is not a FileArchive.");
                fishArchive.close();
                this.isParsed = false;
                return;
            }
            fishArchive.seek(this.file.length() - 8);
            entryCount = fishArchive.readInt();

            byte[] magicBytes = new byte[4];
            fishArchive.readFully(magicBytes);
            String magic = new String(magicBytes, StandardCharsets.UTF_8);

            try {
                this.archiveType = ArchiveType.valueOf(magic);
            } catch (Exception e) {
                System.out.println(magic + " is not a valid FileArchive type.");
                fishArchive.close();
                this.isParsed = false;
                return;
            }

            System.out.println("Entry Count: " + entryCount);
            switch (this.archiveType) {
                case FARC:
                    this.tableOffset = this.file.length() - 0x8 - (entryCount * 0x1C);
                    break;
                case FAR4:
                    this.tableOffset = this.file.length() - 0x1C - (entryCount * 0x1C);
                    break;
                case FAR5:
                    this.tableOffset = this.file.length() - 0x20 - (entryCount * 0x1C);
                    break;
            }

            this.hashTable = new byte[entryCount * 0x1C];

            fishArchive.seek(this.tableOffset);
            fishArchive.read(this.hashTable);

            if (this.archiveType != ArchiveType.FARC) {
                int fatSize = this.archiveType == ArchiveType.FAR4 ?
                    0x84 : 0xAC;
                fishArchive.seek(this.tableOffset - fatSize);
                this.fat = new byte[fatSize];
                fishArchive.read(this.fat);

                if (this.archiveType == ArchiveType.FAR4)
                    fishArchive.seek(this.file.length() - 0x1c);
                else
                    fishArchive.seek(this.file.length() - 0x20);

                fishArchive.read(this.hashinate);
            }

            fishArchive.close();
        } catch (IOException ex) {
            System.err.println(String.format("There was an error processing the %s file!", this.archiveType.name()));
            isParsed = false;
            return;
        }
        Data table = new Data(this.hashTable);
        this.entries = new ArrayList < FileEntry > (entryCount);
        for (int i = 0; i < entryCount; i++)
            this.entries.add(new FileEntry(table
                .bytes(20), table
                .uint32(), table
                .int32(), null));
        long end = System.currentTimeMillis();
        System.out.println(
            String.format("Finished processing %s! (%s s %s ms)",
                this.archiveType.name(),
                ((end - begin) / 1000),
                (end - begin))
        );
        this.isParsed = true;
    }

    public FileEntry find(byte[] hash) {
        return find(hash, false);
    }
    public FileEntry find(byte[] hash, boolean log) {
        for (int i = 0; i < this.entries.size(); i++) {
            if (Arrays.equals(hash, (this.entries.get(i)).hash))
                return this.entries.get(i);
        }
        if (log)
            System.out.println("Could not find entry with SHA1: " + Bytes.toHex(hash));
        return null;
    }

    public void add(byte[] data) {
        byte[] hash = Bytes.SHA1(data);
        if (find(hash, false) != null) return;
        queueSize += (0x1C + data.length);

        FileEntry entry = new FileEntry(data, hash);
        this.entries.add(entry);

        queue.add(new FileEntry(data, hash));
        shouldSave = true;
    }

    public byte[] extract(byte[] hash) {
        return extract(find(hash));
    }

    public byte[] extract(FileEntry entry) {
        if (entry == null)
            return null;
        if (entry.data != null) return entry.data;
        try {
            RandomAccessFile fishArchive = new RandomAccessFile(this.file.getAbsolutePath(), "rw");
            fishArchive.seek(entry.offset);
            byte[] buffer = new byte[entry.size];
            fishArchive.read(buffer);
            fishArchive.close();
            entry.data = buffer;
            return buffer;
        } catch (FileNotFoundException ex) {
            Logger.getLogger(FileArchive.class.getName()).log(Level.SEVERE, (String) null, ex);
        } catch (IOException ex) {
            Logger.getLogger(FileArchive.class.getName()).log(Level.SEVERE, (String) null, ex);
        }
        return null;
    }

    public boolean save() {
        return save(null);
    }
    public boolean save(JProgressBar bar) {
        try {
            if (queue.size() == 0) {
                System.out.println("FileArchive has no items in queue, skipping save.");
                return true;
            }

            System.out.println("Saving FileArchive at " + file.getAbsolutePath());

            if (bar != null) {
                bar.setVisible(true);
                bar.setMaximum(queue.size());
                bar.setValue(0);
            }

            long offset = this.tableOffset;
            Output output = new Output(queueSize + this.hashTable.length + 0xFF, 0);

            for (int i = 0; i < queue.size(); ++i) {
                output.bytes(queue.get(i).data);
                if (bar != null) bar.setValue(i + 1);
            }

            if (this.archiveType != ArchiveType.FARC) {
                offset -= this.fat.length;
                if ((offset + output.offset) % 4 != 0)
                    output.pad(4 - (((int) offset + output.offset) % 4)); // padding for xxtea encryption
                output.bytes(this.fat);
            }

            for (int i = 0; i < queue.size(); ++i) {
                FileEntry entry = queue.get(i);
                output.bytes(entry.hash);
                output.int32((int) offset);
                output.int32(entry.size);
                if (bar != null) bar.setValue(i + 1);
            }

            output.bytes(this.hashTable);

            if (this.archiveType != ArchiveType.FARC)
                output.bytes(this.hashinate);

            if (this.archiveType == ArchiveType.FAR5)
                output.int32(0); // unsure what this is

            output.int32(this.entries.size());
            output.string(this.archiveType.toString());
            output.shrinkToFit();

            RandomAccessFile fileArchive = new RandomAccessFile(this.file.getAbsolutePath(), "rw");
            fileArchive.seek(offset);
            fileArchive.write(output.buffer);
            fileArchive.close();

            shouldSave = false;

            System.out.println("Successfully saved " + queue.size() + " entries to the FileArchive.");
            queue.clear();
            queueSize = 0;

            if (bar != null) {
                bar.setValue(0);
                bar.setMaximum(0);
                bar.setVisible(false);
            }

            process();

        } catch (IOException ex) {
            System.err.println("There was an error saving the FileArchive.");
            Logger.getLogger(FileArchive.class.getName()).log(Level.SEVERE, (String) null, ex);
            return false;
        }
        return true;
    }
}