/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jdbi.v3.sqlobject;

import java.util.List;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;
import org.jdbi.v3.sqlobject.transaction.Transaction;
import org.jdbi.v3.testing.JdbiRule;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class NestedTransactionTest {
    @Rule
    public JdbiRule db = JdbiRule.sqlite().withPlugin(new SqlObjectPlugin());

    @Before
    public void before() {
        db.getHandle().createUpdate("create table foo(bar int primary key)").execute();
    }

    @Test
    public void nestedTransactionsWork() {
        Top top = db.getHandle().attach(Top.class);

        assertThat(top.select()).isEmpty();

        assertThatThrownBy(top::breakTx).isNotNull();

        assertThat(top.select()).isEmpty();
    }

    public interface Top {
        @CreateSqlObject
        Sub sub();

        @SqlUpdate("insert into foo(bar) values(1)")
        void insert();

        @SqlQuery("select bar from foo")
        List<Integer> select();

        @Transaction
        default void breakTx() {
            Top top = this;
            Sub sub = sub();

            assertThatCode(top::insert).doesNotThrowAnyException();
            assertThat(select()).containsExactly(1);

            sub.insert();
            Assert.fail("should have thrown");
        }
    }

    public interface Sub {
        @SqlUpdate("insert into foo(bar) values(1)")
        void insert();
    }
}
