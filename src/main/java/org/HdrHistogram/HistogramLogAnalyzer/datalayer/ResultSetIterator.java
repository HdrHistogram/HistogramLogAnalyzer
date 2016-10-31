/**
 * Written by Azul Systems, and released to the public domain,
 * as explained at http://creativecommons.org/publicdomain/zero/1.0/
 */

package org.HdrHistogram.HistogramLogAnalyzer.datalayer;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Iterator;

abstract class ResultSetIterator<T> implements Iterator {

    ResultSet resultSet;
    private boolean didNext = false;
    private boolean hasNext = false;

    ResultSetIterator(ResultSet resultSet) {
        this.resultSet = resultSet;
    }

    @Override
    public T next() {
        if (!didNext) {
            try {
                resultSet.next();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        didNext = false;

        return nextObject();
    }

    abstract T nextObject();

    @Override
    public boolean hasNext() {
        if (!didNext) {
            try {
                hasNext = resultSet.next();
            } catch (SQLException e) {
                e.printStackTrace();
            }
            didNext = true;
        }
        return hasNext;
    }

    @Override
    public void remove() {
        throw new UnsupportedOperationException();
    }
}
