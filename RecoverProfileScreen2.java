import com.intellij.history.core.LocalHistoryStorage;
import com.intellij.openapi.vfs.newvfs.persistent.FSRecordsImpl;
import com.intellij.util.io.DataInputOutputUtil;
import com.intellij.util.io.IOUtil;

import java.io.*;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

public class RecoverProfileScreen2 {
  static String readString(DataInput in) throws IOException { return IOUtil.readUTF(in); }
  static String readStringOrNull(DataInput in) throws IOException { return in.readBoolean() ? readString(in) : null; }

  static void skipEntry(DataInput in) throws IOException {
    int type = DataInputOutputUtil.readINT(in);
    readString(in); // name
    if (type == 0) {
      in.readLong();
      in.readBoolean();
      DataInputOutputUtil.readINT(in); // contentId
    } else if (type == 1) {
      int count = DataInputOutputUtil.readINT(in);
      for (int i = 0; i < count; i++) skipEntry(in);
    } else {
      throw new IOException("Unknown entry type: " + type);
    }
  }

  public static void main(String[] args) throws Exception {
    String target = args[0].replace('\\', '/');
    Path systemDir = Path.of(args[1]);
    Path storageFile = systemDir.resolve("LocalHistory").resolve("changes");
    Path cachesDir = systemDir.resolve("caches");

    FSRecordsImpl fs = FSRecordsImpl.connect(cachesDir);
    Method readContentById = FSRecordsImpl.class.getDeclaredMethod("readContentById", int.class);
    readContentById.setAccessible(true);

    LocalHistoryStorage storage = new LocalHistoryStorage(storageFile); try {
      int record = storage.getLastRecord();
      int found = 0;
      while (record != 0 && found < 30) {
        try (DataInputStream in = storage.readStream(record)) {
          int version = DataInputOutputUtil.readINT(in);
          long changeSetId = DataInputOutputUtil.readLONG(in);
          String name = readStringOrNull(in);
          long ts = DataInputOutputUtil.readTIME(in);
          if (version >= 1) {
            readStringOrNull(in); // activity kind
            readStringOrNull(in); // activity provider
          }
          int count = DataInputOutputUtil.readINT(in);
          for (int i = 0; i < count; i++) {
            int type = DataInputOutputUtil.readINT(in);
            long changeId = DataInputOutputUtil.readLONG(in);
            switch (type) {
              case 1:
              case 2: {
                String path = readString(in);
                break;
              }
              case 3: {
                String path = readString(in);
                int contentId = DataInputOutputUtil.readINT(in);
                long oldTs = DataInputOutputUtil.readTIME(in);
                if (target.equals(path)) {
                  byte[] bytes;
                  try (InputStream contentIn = (InputStream) readContentById.invoke(fs, contentId)) {
                    bytes = contentIn.readAllBytes();
                  }
                  String text = new String(bytes, StandardCharsets.UTF_8);
                  boolean hasNew = text.contains("DraggableColorSlider") || text.contains("SculptedProfileHero") || text.contains("ProfileModeFab") || text.contains("InlineProfileField");
                  String firstLine = text.lines().findFirst().orElse("");
                  System.out.println("MATCH #" + found + " record=" + record + " changeSetId=" + changeSetId + " ts=" + ts + " name=" + name + " contentId=" + contentId + " len=" + bytes.length + " hasNew=" + hasNew + " firstLine=" + firstLine);
                  found++;
                }
                break;
              }
              case 4: {
                String path = readString(in);
                String oldName = readString(in);
                break;
              }
              case 5: {
                String path = readString(in);
                in.readBoolean();
                break;
              }
              case 6: {
                String path = readString(in);
                String oldPath = readString(in);
                break;
              }
              case 7: {
                String path = readString(in);
                skipEntry(in);
                break;
              }
              case 8: {
                String label = readString(in);
                String projectId = readString(in);
                break;
              }
              case 9: {
                String label = readString(in);
                String projectId = readString(in);
                DataInputOutputUtil.readSINT(in);
                break;
              }
              default:
                throw new IOException("Unknown change type: " + type + " at record " + record + ", changeId=" + changeId);
            }
          }
        }
        record = storage.getPrevRecord(record);
      }
    } finally {
      fs.close();
    }
  }
}
