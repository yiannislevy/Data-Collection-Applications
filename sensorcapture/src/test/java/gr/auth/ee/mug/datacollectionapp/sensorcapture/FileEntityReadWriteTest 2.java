package gr.auth.ee.mug.datacollectionapp.sensorcapture;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

import android.content.Context;

import androidx.room.Room;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.util.List;

import gr.auth.ee.mug.datacollectionapp.sensorcapture.Database.AppDatabase;
import gr.auth.ee.mug.datacollectionapp.sensorcapture.Database.FileDAO;
import gr.auth.ee.mug.datacollectionapp.sensorcapture.Database.FileEntity;

@RunWith(AndroidJUnit4.class)
public class FileEntityReadWriteTest {
    private FileDAO fileDao;
    private AppDatabase db;

    @Before
    public void createDb() {
        Context context = ApplicationProvider.getApplicationContext();
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase.class).build();
        fileDao = db.fileDao();
    }

    @After
    public void closeDb() throws IOException {
        db.close();
    }

    @Test
    public void writeFileEntityAndReadInList() throws Exception {
        FileEntity fileEntity = new FileEntity("test_file", "pending", System.currentTimeMillis());
        fileDao.insert(fileEntity);
        List<FileEntity> byName = fileDao.getFilesByStatus("pending");
        assertThat(byName.get(0), equalTo(fileEntity));
    }
}
