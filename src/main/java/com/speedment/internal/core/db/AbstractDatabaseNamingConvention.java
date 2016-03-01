/*
 * Copyright 2016 Speedment, Inc..
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.speedment.internal.core.db;

import com.speedment.config.Document;
import com.speedment.config.db.Column;
import com.speedment.config.db.Schema;
import com.speedment.config.db.Table;
import com.speedment.db.DatabaseNamingConvention;
import com.speedment.exception.SpeedmentException;
import java.util.Set;

/**
 * An abstract base implementation of the {@link DatabaseNamingConvention} 
 * interface that works for most relational databases.
 * 
 * @author Emil Forslund
 */
public abstract class AbstractDatabaseNamingConvention 
    implements DatabaseNamingConvention {

    @Override
    public String fullNameOf(Column column) {
        return column.getParent().map(Table::getName)
            .orElseThrow(() -> noParentException(column))
            + "." + column.getName();
    }

    @Override
    public String fullNameOf(Table table) {
        return table.getParent().map(Schema::getName)
            .orElseThrow(() -> noParentException(table))
            + "." + table.getName();
    }

    @Override
    public String quoteField(String field) {
        return getFieldQuoteStart() + field + getFieldQuoteEnd();
    }
    
    @Override
    public String encloseField(String field) {
        return getFieldEncloserStart() + field + getFieldEncloserEnd();
    }
    
    @Override
    public abstract Set<String> getSchemaExcludeSet();
    
    /**
     * Returns the non-null field quote start string. The field quote
     * start string precedes a database value used in among other things 
     * comparisons.
     *
     * @return the non-null field quote start string
     * @see #getFieldQuoteEnd()
     */
    protected abstract String getFieldQuoteStart();
    
    /**
     * Returns the non-null field quote end string. The field quote
     * end string precedes a database value used in among other things 
     * comparisons.
     *
     * @return the non-null field quote end string
     * @see #getFieldQuoteEnd()
     */
    protected abstract String getFieldQuoteEnd();
    
    /**
     * Returns the non-null field encloser start string. The field encloser
     * start string precedes a database entity name like a table or schema name
     * when quoted. Quoted names are used to avoid that entity names collide
     * with reserved keywords like "key" or "user". So a table named "user" in
     * the "key" schema can be quoted to "key"."user". Examples of values are
     * '`' for MySQL or '"' for Oracle.
     *
     * @return the non-null field encloser start string
     *
     * @see #getFieldEncloserStart(boolean)
     * @see #getFieldEncloserEnd()
     * @see #getFieldEncloserEnd(boolean)
     */
    protected abstract String getFieldEncloserStart();
    
    /**
     * Returns the non-null field encloser start string. The method parameter
     * denotes if the field encloser is placed within quotes or not. For example
     * for Oracle, since the field encloser is the '"' character itself, it
     * needs to be escaped if within quotes.
     *
     * @param isWithinQuotes if the field encloser is within quotes
     * @return Returns the non-null field encloser start string
     *
     * @see #getFieldEncloserStart()
     */
    protected String getFieldEncloserStart(boolean isWithinQuotes) {
        return escapeIfQuote(getFieldEncloserStart(), isWithinQuotes);
    }
    
    /**
     * Returns the non-null field encloser end string. The field encloser end
     * string follows a database entity name like a table or schema name when
     * quoted. Quoted names are used to avoid that entity names collide with
     * reserved keywords like "key" or "user". So a table named "user" in the
     * "key" schema can be quoted to "key"."user". Examples of values are '`'
     * for MySQL or '"' for Oracle.
     *
     * @return the non-null field encloser end string
     *
     * @see #getFieldEncloserStart(boolean)
     * @see #getFieldEncloserEnd()
     * @see #getFieldEncloserEnd(boolean)
     */
    protected abstract String getFieldEncloserEnd();

    /**
     * Returns the non-null field encloser end string. The method parameter
     * denotes if the field encloser is placed within quotes or not. For example
     * for Oracle, since the field encloser is the '"' character itself, it
     * needs to be escaped if within quotes.
     *
     * @param isWithinQuotes if the field encloser is within quotes
     * @return Returns the non-null field encloser start string
     *
     * @see #getFieldEncloserEnd()
     */
    protected String getFieldEncloserEnd(boolean isWithinQuotes) {
        return escapeIfQuote(getFieldEncloserEnd(), isWithinQuotes);
    }
    
    private String escapeIfQuote(String item, boolean isWithinQuotes) {
        if (isWithinQuotes && "\"".equals(item)) {
            return "\\" + item;
        } else return item;
    }
    
    private final static SpeedmentException noParentException(Document doc) {
        return new SpeedmentException(
            "Can't create full name of '" + doc + 
            "' since it doesn't have a parent."
        );
    }
}