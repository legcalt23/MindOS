import com.intellij.history.core.*;
import com.intellij.history.core.changes.*;
import com.intellij.mock.MockApplication;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.PathManager;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.vfs.newvfs.ManagingFS;
import com.intellij.openapi.vfs.newvfs.persistent.PersistentFS;
import com.intellij.openapi.vfs.newvfs.persistent.PersistentFSImpl;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;

import java.nio.file.*;

public class RecoverProfileScreen {
  public static void main(String[] args) throws Exception {
    String target = args[0].replace('\\', '/');
    Path systemDir = Path.of(args[1]);
    Path storageDir = systemDir.resolve("LocalHistory");
    Path storageFile = storageDir.resolve("changes");

    System.setProperty(PathManager.PROPERTY_HOME_PATH, "C:/Program Files/Android/Android Studio");
    System.setProperty(PathManager.PROPERTY_SYSTEM_PATH, systemDir.toString());

    LocalHistoryStorage storage = new LocalHistoryStorage(storageFile);
    long fsTs = storage.getFSTimestamp();
    Disposer.dispose(storage);

    Disposable disposable = Disposer.newDisposable();
    MockApplication app = MockApplication.setUp(disposable);
    ApplicationManager.setApplication(app, disposable);

    PersistentFSImpl pfs = new PersistentFSImpl(app);
    app.registerService(PersistentFS.class, pfs);
    app.registerService(ManagingFS.class, pfs);
    pfs.connect();

    ChangeListStorageImpl cls = new ChangeListStorageImpl(storageDir);
    IntSet guard = new IntOpenHashSet();
    ChangeSetHolder holder = cls.readPrevious(-1, guard);
    int found = 0;
    while (holder != null && found < 20) {
      ChangeSet cs = holder.changeSet();
      for (Object obj : cs.getChanges()) {
        Change change = (Change)obj;
        if (change instanceof StructuralChange sc && target.equals(sc.getPath())) {
          if (change instanceof ContentChange cc) {
            byte[] bytes = cc.getOldContent().getBytes();
            String text = new String(bytes);
            boolean hasNew = text.contains("DraggableColorSlider") || text.contains("SculptedProfileHero") || text.contains("ProfileModeFab") || text.contains("InlineProfileField");
            String firstLine = text.lines().findFirst().orElse("");
            System.out.println("MATCH #" + found + " record=" + holder.id() + " ts=" + cs.getTimestamp() + " len=" + bytes.length + " hasNew=" + hasNew + " firstLine=" + firstLine);
            found++;
          } else {
            System.out.println("MATCH structural non-content: " + change.getClass().getName() + " record=" + holder.id() + " ts=" + cs.getTimestamp());
          }
        }
      }
      holder = cls.readPrevious(holder.id(), guard);
    }

    cls.close();
    pfs.disconnect();
    Disposer.dispose(disposable);
  }
}
