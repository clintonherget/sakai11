/**********************************************************************************
 *
 * Copyright (c) 2019 The Sakai Foundation
 *
 * Original developers:
 *
 *   New York University
 *   Payten Giles
 *   Mark Triggs
 *
 * Licensed under the Educational Community License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.osedu.org/licenses/ECL-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 **********************************************************************************/

package edu.nyu.classes.seats.storage;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * Provide an iterator over a ResultSet.
 */
public class DBResults implements Iterable<ResultSet>, Iterator<ResultSet>, AutoCloseable {
    private final PreparedStatement originalStatement;
    private final ResultSet resultSet;
    private boolean hasRowReady;

    public DBResults(ResultSet rs, PreparedStatement originalStatement) {
        this.resultSet = rs;
        this.originalStatement = originalStatement;
    }

    @Override
    public void close() throws SQLException {
        resultSet.close();
        originalStatement.close();
    }

    @Override
    public boolean hasNext() {
        try {
            if (!hasRowReady) {
                hasRowReady = resultSet.next();
            }

            return hasRowReady;
        } catch (SQLException e) {
            return false;
        }
    }

    @Override
    public ResultSet next() {
        if (!hasRowReady) {
            throw new NoSuchElementException("Read past end of results");
        }

        hasRowReady = false;
        return resultSet;
    }

    @Override
    public Iterator<ResultSet> iterator() {
        return this;
    }

    public Stream<ResultSet> stream() {
        return StreamSupport.stream(Spliterators.spliteratorUnknownSize(this, Spliterator.ORDERED),
                                    false);
    }

    public List<String> getStringColumn(String column) {
        return this
            .stream()
            .map(r -> {
                    try {
                        return r.getString("group_id");
                    } catch (SQLException e) {
                        throw new RuntimeException(e);
                    }
                })
            .collect(Collectors.toList());
    }

    public int getCount() throws SQLException {
        if (this.hasNext()) {
            return this.resultSet.getInt(0);
        } else {
            return 0;
        }
    }
}
