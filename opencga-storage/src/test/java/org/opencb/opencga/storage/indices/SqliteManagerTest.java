package org.opencb.opencga.storage.indices;

import org.junit.Test;
import org.opencb.commons.test.GenericTest;
import org.opencb.opencga.lib.common.XObject;

import java.nio.file.Paths;
import java.util.Iterator;

/**
 * Created by aaleman on 12/18/13.
 */
public class SqliteManagerTest extends GenericTest {


    @Test
    public void testQueryIterator() throws Exception {

        String db = "/opt/data/test.sqlite3";

        SqliteManager sm = new SqliteManager();
        sm.connect(Paths.get(db), true);

        Iterator<XObject> it = sm.queryIterator("Select * from variant;");

        int cont = 0;
        while (it.hasNext()) {
            XObject xobj = it.next();
            System.out.println(xobj);
            cont++;
        }
        System.out.println(cont);


    }
}
